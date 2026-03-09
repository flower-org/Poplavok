package com.poplavok.kucoin;

import com.KyKu4.MogeJlb.exception.KucoinApiException;
import com.KyKu4.MogeJlb.response.AllTickersResponse;
import com.KyKu4.MogeJlb.response.ApiCurrencyDetailChainPropertyResponse;
import com.KyKu4.MogeJlb.response.CurrencyDetailV2Response;
import com.KyKu4.MogeJlb.response.CurrencyExtendedInfoResponse;
import com.KyKu4.MogeJlb.response.CurrencyResponse;
import com.KyKu4.MogeJlb.response.MarketTickerResponse;
import com.KyKu4.npo4ee.KyKu4_3anpocHuK;
import com.KyKu4.npo4ee.KyKu4_XTTn;
import com.KyKu4.npo4ee.PublicAPIPack;
import com.flower.fxutils.ProgressCallback;
import com.poplavok.data.dao.ChainDAO;
import com.poplavok.data.dao.CurrencyChainDAO;
import com.poplavok.data.dao.CurrencyDAO;
import com.poplavok.data.dao.CurrencyExtendedInfoDAO;
import com.poplavok.data.dao.MarketTickerDAO;
import com.poplavok.data.model.Chain;
import com.poplavok.data.model.Currency;
import com.poplavok.data.model.CurrencyChain;
import com.poplavok.data.model.CurrencyExtendedInfo;
import com.poplavok.data.model.MarketTicker;
import com.poplavok.data.utils.DBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class KucoinTool {
    final static Logger LOGGER = LoggerFactory.getLogger(KucoinTool.class);

    final static PublicAPIPack PUBLIC_API_PACK = KyKu4_XTTn.getPublicAPIPack();

    public static void retrieveCurrencyFromExchange(ProgressCallback progressCallback) {
        double maxProgress = 1.0;
        try {
            retrieveCurrencyFromExchange(progressCallback, maxProgress);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), maxProgress, true);
        }
    }

    private static void retrieveCurrencyFromExchange(ProgressCallback progressCallback, double maxProgress) throws ExecutionException, InterruptedException, IOException {
        double progress = 0.01*maxProgress;
        progressCallback.updateProgress("Loading currencies from DB", progress, false);

        //1. Currencies
        Map<String, Currency> currenciesMap = DBUtil.connectGetResultAndClose(CurrencyDAO::findAll).stream().collect(Collectors.toMap(Currency::getCurrency, c -> c));

        progress = 0.02*maxProgress;
        progressCallback.updateProgress("Retrieving currencies from exchange", progress, false);

        List<CurrencyResponse> currenciesResponse = KyKu4_3anpocHuK.getCurrencies(PUBLIC_API_PACK.currencyAPI());

        double progressPerCurrencyStep = (maxProgress - progress) / (currenciesResponse.size() + 1);
        for (int i = 0; i < currenciesResponse.size(); i++) {
            CurrencyResponse currencyResponse = currenciesResponse.get(i);
            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, currenciesResponse.size(), currencyResponse.currency()), progress, false);

            //Upsert currency
            Currency curr = currenciesMap.get(currencyResponse.currency());
            if (curr == null) {
                //Insert
                DBUtil.connectCommitAndClose(conn -> CurrencyDAO.save(conn, EntityConverter.fromResponse(currencyResponse)));
            } else {
                //Update
                Currency retrievedCurrency = EntityConverter.fromResponse(curr.getId(), currencyResponse);
                if (!retrievedCurrency.equals(curr)) {
                    DBUtil.connectCommitAndClose(conn -> CurrencyDAO.update(conn, retrievedCurrency));
                }
            }
        }

        progressCallback.updateProgress("Currency retrieval Done", maxProgress, true);
    }

    // ------------------------------------------------------------

    public static void retrieveMarketTickersFromExchange(ProgressCallback progressCallback) {
        double maxProgress = 1.0;
        double split = 0.2;
        try {
            retrieveCurrencyFromExchange(progressCallback, split);
            retrieveMarketTickersFromExchange(progressCallback, split);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), maxProgress, true);
        }
    }

    private static void retrieveMarketTickersFromExchange(ProgressCallback progressCallback, final double startFrom) throws ExecutionException, InterruptedException, IOException {
        final double maxProgress = 1.0;
        double progress = startFrom;

        //Reload currencies
        progress += 0.1;
        progressCallback.updateProgress("Re-Loading currencies from DB", progress, false);
        Map<String, Currency> currenciesMap = DBUtil.connectGetResultAndClose(CurrencyDAO::findAll).stream().collect(Collectors.toMap(Currency::getCurrency, c -> c));

        //MarketTickers
        progress += 0.1;
        progressCallback.updateProgress("Loading market tickers from DB", progress, false);
        List<MarketTicker> marketTickers = DBUtil.connectGetResultAndClose(MarketTickerDAO::findAll);
        Map<String, MarketTicker> marketTickersBySymbol = marketTickers.stream().collect(Collectors.toMap(MarketTicker::getSymbol, m -> m));

        progress += 0.1;
        progressCallback.updateProgress("Retrieving market tickers from exchange", progress, false);
        AllTickersResponse tickers = KyKu4_3anpocHuK.getAllTickers(PUBLIC_API_PACK.symbolAPI());

        int tickerSize = checkNotNull(tickers.ticker()).size();
        double progressPerCurrencyStep = (maxProgress - progress) / (tickerSize + 1);
        for (int i = 0; i < tickerSize; i++) {
            MarketTickerResponse marketTickerResponse = tickers.ticker().get(i);
            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, tickerSize, marketTickerResponse.symbol()), progress, false);

            String[] symbolParts = marketTickerResponse.symbol().split("-");

            Currency baseCurrency = currenciesMap.get(symbolParts[0]);
            Currency quoteCurrency = currenciesMap.get(symbolParts[1]);
            if (baseCurrency != null && quoteCurrency != null) {
                MarketTicker ticker = marketTickersBySymbol.get(marketTickerResponse.symbol());
                if (ticker == null) {
                    //insert
                    DBUtil.connectCommitAndClose(conn -> MarketTickerDAO.save(conn, EntityConverter.fromResponse(baseCurrency.getId(), quoteCurrency.getId(), marketTickerResponse)));
                } else {
                    //update
                    DBUtil.connectCommitAndClose(conn -> MarketTickerDAO.update(conn, EntityConverter.fromResponse(ticker.getId(), baseCurrency.getId(), quoteCurrency.getId(), marketTickerResponse)));
                }
            } else {
                LOGGER.warn("Retrieving MarketTickers from Exchange: Can't process MarketTicker: symbol {}, BaseCurrency {} - {}, QuoteCurrency {} - {}", marketTickerResponse.symbol(), symbolParts[0], baseCurrency != null ? "Found" : "Not found", symbolParts[1], quoteCurrency != null ? "Found" : "Not found");
            }
        }

        progressCallback.updateProgress("Market tickers retrieval Done", maxProgress, true);
    }

    // ------------------------------------------------------------

    /*public static void retrieveAccountDetailsFromExchange(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, Long accountId) {
        try {
            progressCallback.updateProgress("Re-Loading internal accounts from Exchange", 0, false);

            retrieveInternalAccounts(progressCallback, privateAPIPack, 0, 0.33, accountId);
            retrieveCrossMarginAccounts(progressCallback, privateAPIPack, 0.33, 0.66, accountId);
            retrieveIsolatedMarginAccounts(progressCallback, privateAPIPack, 0.66, 0.99, accountId);

            progressCallback.updateProgress(String.format("Retrieval Done."), 1.0, true);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), 1.0, true);
        }
    }

    public static void retrieveOrdersFromExchange(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, Long accountId) {
        try {
            progressCallback.updateProgress("Re-Loading orders from Exchange", 0, false);

            retrieveOrders(progressCallback, privateAPIPack, 0, 0.33, accountId);

            progressCallback.updateProgress(String.format("Retrieval Done."), 1.0, true);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), 1.0, true);
        }
    }

    static void retrieveInternalAccounts(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, double startProgress, double maxProgress, Long accountId) throws ExecutionException, InterruptedException, IOException {
        double progress = startProgress + 0.01 * maxProgress;
        progressCallback.updateProgress("Loading Internal Accounts from DB", progress, false);

        //1. Internal Accounts
        Map<String, InternalAccount> internalAccountsMap = DBTool.connectGetResultAndClose(conn -> InternalAccountDAO.getAllForAccount(conn, accountId)).stream().collect(Collectors.toMap(InternalAccount::id, c -> c));

        progress = startProgress + 0.02 * maxProgress;
        progressCallback.updateProgress("Retrieving Internal Accounts from exchange", progress, false);

        List<AccountBalancesResponse> accountBalancesResponses = KyKu4_3anpocHuK.getAccountList(privateAPIPack.accountAPI(), null, null);

        double progressPerCurrencyStep = (maxProgress - progress) / (accountBalancesResponses.size() + 1);
        for (int i = 0; i < accountBalancesResponses.size(); i++) {
            AccountBalancesResponse accountBalancesResponse = accountBalancesResponses.get(i);
            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, accountBalancesResponses.size(), accountBalancesResponse.currency()), progress, false);

            Currency currency = DBTool.connectGetResultAndClose(conn -> CurrencyDAO.getByCurrency(conn, accountBalancesResponse.currency()));

            //Upsert account balance
            InternalAccount internalAccount = internalAccountsMap.get(accountBalancesResponse.id());
            if (internalAccount == null) {
                //Insert
                DBTool.connectCommitAndClose(conn -> InternalAccountDAO.insert(conn, InternalAccount.fromResponse(accountId, currency.currencyId(), accountBalancesResponse)));
            } else {
                //Update
                InternalAccount retrievedInternalAccount = InternalAccount.fromResponse(internalAccount.internalAccountId(), accountId, currency.currencyId(), accountBalancesResponse);
                if (!internalAccount.equals(retrievedInternalAccount)) {
                    DBTool.connectCommitAndClose(conn -> InternalAccountDAO.update(conn, retrievedInternalAccount));
                }
            }
        }

        progressCallback.updateProgress("InternalAccounts retrieval Done", maxProgress, true);
    }

    static void retrieveOrders(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, double startProgress, double maxProgress, Long accountId) throws ExecutionException, InterruptedException, IOException {
        Pagination<OrderResponse> firstPage = KyKu4_3anpocHuK.getOrderList(privateAPIPack.orderAPI(), OrderAPIRetrofit.TradeType.MARGIN_ISOLATED_TRADE, "active",null, null);

        int pageSize = firstPage.pageSize();
        int currentPageNum = firstPage.currentPage();
        int totalPages = firstPage.totalPage();

        Pagination<OrderResponse> nextPage =  KyKu4_3anpocHuK.getOrderList(privateAPIPack.orderAPI(), OrderAPIRetrofit.TradeType.MARGIN_ISOLATED_TRADE, "active", pageSize, ++currentPageNum);

        int totalPages2 = firstPage.totalPage();

        //for ( int i = 1; i < totalPages; i++) {
        //    Pagination<OrderResponse> firstPage = KyKu4_3anpocHuK.getOrderList(privateAPIPack.orderAPI(), null, null);
        //}

        //


//        double progress = startProgress + 0.01 * maxProgress;
//        progressCallback.updateProgress("Loading Internal Accounts from DB", progress, false);
//
//
//
//        //1. Internal Accounts
//        //Map<String, InternalAccountOrder> accountOrdersMap = DBTool.connectGetResultAndClose(conn -> InternalAccountDAO.getAllForAccount(conn, accountId)).stream().collect(Collectors.toMap(InternalAccount::id, c -> c));
//
//        progress = startProgress + 0.02 * maxProgress;
//        progressCallback.updateProgress("Retrieving Internal Accounts from exchange", progress, false);
//
//        List<AccountBalancesResponse> accountBalancesResponses = KyKu4_3anpocHuK.getAccountList(privateAPIPack.accountAPI(), null, null);
//
//        double progressPerCurrencyStep = (maxProgress - progress) / (accountBalancesResponses.size() + 1);
//        for (int i = 0; i < accountBalancesResponses.size(); i++) {
//            AccountBalancesResponse accountBalancesResponse = accountBalancesResponses.get(i);
//            progress += progressPerCurrencyStep;
//            progressCallback.updateProgress(
//                    String.format("(%d/%d) Updating %s", i + 1, accountBalancesResponses.size(), accountBalancesResponse.currency()), progress, false);
//
//            Currency currency = DBTool.connectGetResultAndClose(conn -> CurrencyDAO.getByCurrency(conn, accountBalancesResponse.currency()));
//
//            //Upsert account balance
//            InternalAccount internalAccount = internalAccountsMap.get(accountBalancesResponse.id());
//            if (internalAccount == null) {
//                //Insert
//                DBTool.connectCommitAndClose(conn -> InternalAccountDAO.insert(conn, InternalAccount.fromResponse(accountId, currency.currencyId(), accountBalancesResponse)));
//            } else {
//                //Update
//                InternalAccount retrievedInternalAccount = InternalAccount.fromResponse(internalAccount.internalAccountId(), accountId, currency.currencyId(), accountBalancesResponse);
//                if (!internalAccount.equals(retrievedInternalAccount)) {
//                    DBTool.connectCommitAndClose(conn -> InternalAccountDAO.update(conn, retrievedInternalAccount));
//                }
//            }
//        }
    }*/
/*
    static void retrieveCrossMarginAccounts(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, double startProgress, double maxProgress, Long accountId) throws ExecutionException, InterruptedException, IOException {
        double progress = startProgress + 0.01 * maxProgress;
        progressCallback.updateProgress("Loading Cross-Margin Accounts from DB", progress, false);

        //1. Cross-Margin Accounts
        Map<Long, CrossMarginAccount> crossMarginAccountMap = DBTool.connectGetResultAndClose(conn -> CrossMarginAccountDAO.getAllForAccount(conn, accountId)).stream().collect(Collectors.toMap(CrossMarginAccount::currencyId, c -> c));

        progress = startProgress + 0.02 * maxProgress;
        progressCallback.updateProgress("Retrieving Cross-Margin Accounts from exchange", progress, false);

        MarginAccountResponse marginAccountResponses = KyKu4_3anpocHuK.getMarginAccount(privateAPIPack.marginAPI());
        List<MarginAccount> marginAccounts = marginAccountResponses.accounts();

        double progressPerCurrencyStep = (maxProgress - progress) / (marginAccounts.size() + 1);
        for (int i = 0; i < marginAccounts.size(); i++) {
            MarginAccount marginAccountResponse = marginAccounts.get(i);
            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, marginAccounts.size(), marginAccountResponse.currency()), progress, false);

            Currency currency = DBTool.connectGetResultAndClose(conn -> CurrencyDAO.getByCurrency(conn, marginAccountResponse.currency()));

            //Upsert account balancex
            CrossMarginAccount crossMarginAccount = crossMarginAccountMap.get(currency.currencyId());
            if (crossMarginAccount == null) {
                //Insert
                DBTool.connectCommitAndClose(conn -> CrossMarginAccountDAO.insert(conn, CrossMarginAccount.fromResponse(accountId, currency.currencyId(), marginAccountResponse)));
            } else {
                //Update
                CrossMarginAccount retrievedCrossMarginAccount = CrossMarginAccount.fromResponse(crossMarginAccount.crossMarginAccountId(), accountId, currency.currencyId(), marginAccountResponse);
                if (!crossMarginAccount.equals(retrievedCrossMarginAccount)) {
                    DBTool.connectCommitAndClose(conn -> CrossMarginAccountDAO.update(conn, retrievedCrossMarginAccount));
                }
            }
        }

        progressCallback.updateProgress("Cross-Margin Accounts retrieval Done", maxProgress, true);
    }

    static void retrieveIsolatedMarginAccounts(ProgressCallback progressCallback, PrivateAPIPack privateAPIPack, double startProgress, double maxProgress, Long accountId) throws ExecutionException, InterruptedException, IOException {
        double progress = startProgress + 0.01 * maxProgress;
        progressCallback.updateProgress("Loading Isolated Margin Accounts from DB", progress, false);

        //1. Isolated Margin Accounts
        Map<Long, IsolatedMarginAccount> isolatedMarginAccountMap = DBTool.connectGetResultAndClose(conn -> IsolatedMarginAccountDAO.getAllForAccount(conn, accountId)).stream().collect(Collectors.toMap(IsolatedMarginAccount::marketTickerId, c -> c));

        progress = startProgress + 0.02 * maxProgress;
        progressCallback.updateProgress("Retrieving Isolated Margin Accounts from exchange", progress, false);

        IsolatedMarginAccountInfo isolatedMarginAccountInfo = KyKu4_3anpocHuK.getIsolatedMarginAccountInfo(privateAPIPack.marginAPI(), null);
        List<IsolatedMarginAccountInfo.Asset> assets = isolatedMarginAccountInfo.assets();

        double progressPerCurrencyStep = (maxProgress - progress) / (assets.size() + 1);
        for (int i = 0; i < assets.size(); i++) {
            IsolatedMarginAccountInfo.Asset asset = assets.get(i);
            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, assets.size(), asset.symbol()), progress, false);

            MarketTicker marketTicker = DBTool.connectGetResultAndClose(conn -> MarketTickerDAO.getBySymbol(conn, asset.symbol()));

            //Upsert account balance
            IsolatedMarginAccount isolatedMarginAccount = isolatedMarginAccountMap.get(marketTicker.marketTickerId());
            if (isolatedMarginAccount == null) {
                //Insert
                DBTool.connectCommitAndClose(conn -> IsolatedMarginAccountDAO.insert(conn, IsolatedMarginAccount.fromResponse(accountId, marketTicker.marketTickerId(), asset)));
            } else {
                //Update
                IsolatedMarginAccount retrievedIsolatedMarginAccount = IsolatedMarginAccount.fromResponse(isolatedMarginAccount.isolatedMarginAccountId(), accountId, marketTicker.marketTickerId(), asset);
                if (!isolatedMarginAccount.equals(retrievedIsolatedMarginAccount)) {
                    DBTool.connectCommitAndClose(conn -> IsolatedMarginAccountDAO.update(conn, retrievedIsolatedMarginAccount));
                }
            }
        }

        progressCallback.updateProgress("Isolated Margin Accounts retrieval Done", maxProgress, true);
    }*/

    // ------------------------------------------------------------

    public static void retrieveCurrencyDetailsFromExchange(ProgressCallback progressCallback) {
        double maxProgress = 1.0;
        double split = 0.2;
        try {
            retrieveCurrencyFromExchange(progressCallback, split);
            retrieveCurrencyDetailsFromExchange(progressCallback, split);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), maxProgress, true);
        }
    }

    private static void retrieveCurrencyDetailsFromExchange(ProgressCallback progressCallback, double startFrom) throws ExecutionException, InterruptedException, IOException {
        final double maxProgress = 1.0;
        double progress = startFrom;

        //Reload currencies
        progress += 0.1;
        progressCallback.updateProgress("Re-Loading currencies from Exchange", progress, false);
        List<Currency> currenciesList = DBUtil.connectGetResultAndClose(CurrencyDAO::findAll);

        //currencyId  chainId
        //   ^          ^
        //Map<Long, Map<Long, CurrencyChain>> currencyChainMap = DBTool.connectGetResultAndClose(CurrencyChainDAO::getAll)
        //        .stream().collect(Collectors.groupingBy(CurrencyChain::currencyId, Collectors.toMap(CurrencyChain::chainId, c -> c)));
        //Map<Long, CurrencyExtendedInfo> currencyExtendedInfoMap = DBTool.connectGetResultAndClose(CurrencyExtendedInfoDAO::getAll)
        //        .stream().collect(Collectors.toMap(CurrencyExtendedInfo::currencyExtendedInfoId, c -> c));

        double progressPerCurrencyStep = (maxProgress - progress) / (currenciesList.size() + 1);
        for (int i = 0; i < currenciesList.size(); i++) {
            Currency currency = currenciesList.get(i);

            progress += progressPerCurrencyStep;
            progressCallback.updateProgress(
                    String.format("(%d/%d) Updating %s", i + 1, currenciesList.size(), currency.getCurrency()), progress, false);

            retrieveCurrencyDetailsForCurrency(currency);
        }

        progressCallback.updateProgress("Currency details retrieval Done", maxProgress, true);
    }

    private static void retrieveCurrencyDetailsForCurrency(Currency currency) throws ExecutionException, InterruptedException, IOException {
        retrieveCurrencyDetailsForCurrencyInternal(null, currency);
    }

    public static void retrieveCurrencyDetailsForCurrency(ProgressCallback progressCallback, long currencyId) {
        try {
            progressCallback.updateProgress("Re-Loading currency from DB", 0.25, false);
            Currency currency = DBUtil.connectGetResultAndClose(conn -> CurrencyDAO.findById(conn, currencyId)).get();

            retrieveCurrencyDetailsForCurrencyInternal(progressCallback, currency);
        } catch(Exception e) {
            LOGGER.error("Retrieval Error", e);
            progressCallback.updateProgress(String.format("Retrieval Error: %s", e), 1.0, true);
        }
    }

    private static void retrieveCurrencyDetailsForCurrencyInternal(@Nullable ProgressCallback progressCallback, Currency currency) throws IOException {
        if (progressCallback != null) {
            progressCallback.updateProgress("Retrieving Extended info for " + currency.getCurrency() + " from exchange", 0.5, false);
        }

        //1. ExtendedInfo
        Optional<CurrencyExtendedInfo> currencyExtendedInfoOpt = DBUtil.connectGetResultAndClose(conn -> CurrencyExtendedInfoDAO.findById(conn, currency.getId()));

        int waitMs = 1000;

        CurrencyExtendedInfoResponse currencyExtendedInfoResponseVar = null;
        while (true) {
            try {
                currencyExtendedInfoResponseVar = KyKu4_3anpocHuK.getCurrencyExtendedInfo(PUBLIC_API_PACK.kucoinAPI(), currency.getCurrency());
                break;
            } catch (KucoinApiException e) {
                // Too many requests, wait and retry
                if (Integer.parseInt(e.getCode()) == 429) {
                    try {
                        LOGGER.info("Got 429 for currency {}, waiting for {} ms before retrying", currency.getCurrency(), waitMs);
                        Thread.sleep(waitMs);
                        if (waitMs < 60000) {
                            waitMs = waitMs*2;
                            if (waitMs > 60000) {
                                waitMs = 60000;
                            }
                        }
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                } else { throw new RuntimeException(e); }
            }
        }

        CurrencyExtendedInfoResponse currencyExtendedInfoResponse = checkNotNull(currencyExtendedInfoResponseVar);
        if (currencyExtendedInfoOpt.isEmpty()) {
            //insert
            DBUtil.connectCommitAndClose(conn -> CurrencyExtendedInfoDAO.save(conn, EntityConverter.fromResponse(currency.getId(), currency.getCurrency(), currencyExtendedInfoResponse)));
        } else {
            //update
            DBUtil.connectCommitAndClose(conn -> CurrencyExtendedInfoDAO.update(conn, EntityConverter.fromResponse(currency.getId(), currency.getCurrency(), currencyExtendedInfoResponse)));
        }

        if (progressCallback != null) {
            progressCallback.updateProgress("Retrieving Chains info for " + currency.getCurrency() + " from exchange", 0.75, false);
        }

        //2. CurrencyChains
        Map<Long, CurrencyChain> currencyChainMap = DBUtil.connectGetResultAndClose(conn -> CurrencyChainDAO.getForCurrency(conn, currency.getId()))
                .stream().collect(Collectors.toMap(CurrencyChain::chainId, c -> c));
        CurrencyDetailV2Response currencyDetailV2Response = KyKu4_3anpocHuK.getCurrencyDetailV2(PUBLIC_API_PACK.currencyAPI(), currency.getCurrency());

        if (currencyDetailV2Response.list() != null) {
            for (ApiCurrencyDetailChainPropertyResponse chainResponse : currencyDetailV2Response.list()) {
                Chain chain = DBUtil.connectGetResultAndClose(conn -> ChainDAO.getByChain(conn, chainResponse.chainId()));
                if (chain == null) {
                    DBUtil.connectCommitAndClose(conn -> ChainDAO.save(conn, Chain.ofNew(chainResponse.chainName(), chainResponse.chainId())));
                    chain = DBUtil.connectGetResultAndClose(conn -> ChainDAO.getByChain(conn, chainResponse.chainId()));
                }

                long chainId = checkNotNull(chain.chainId());
                CurrencyChain currencyChain = currencyChainMap.get(chainId);
                if (currencyChain == null) {
                    //insert
                    DBUtil.connectCommitAndClose(conn -> CurrencyChainDAO.save(conn, EntityConverter.fromResponse(currency.getId(), chainId, chainResponse)));
                } else {
                    //update
                    DBUtil.connectCommitAndClose(conn -> CurrencyChainDAO.update(conn, EntityConverter.fromResponse(currencyChain.currencyChainId(), currency.getId(), chainId, chainResponse)));
                }
            }
        } else {
            LOGGER.warn("chains are null: CurrencyDetailV2Response {}", currencyDetailV2Response);
        }

        if (progressCallback != null) {
            progressCallback.updateProgress("Currency details retrieval Done", 1.0, true);
        }
    }
}

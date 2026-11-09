# MultiCoinChartsServer

Standalone Vert.x web server for the [multicoincharts-minimal](https://github.com/flower-org/multicoincharts-minimal)
static site. Can also be embedded via `MultiCoinChartsServer`.

## Site content

The site is served from the classpath directory `multicoincharts-minimal`, i.e.
`src/main/resources/multicoincharts-minimal` (git submodule).

- Dev: the submodule is served as-is (unminified).
- Release (CI only): the submodule's minify action builds `dist/`, everything except `dist/` is deleted and the
  `dist/` contents are moved up into the submodule folder. The server code is the same in both cases.

## Routing

- `/` and directories are served via `index.html`; `/home` redirects to `/home/` so relative links work.
- Extensionless paths fall back to `.html` (`/adv-scanner` → `adv-scanner.html`).
- Hidden files and folders (`.git`, `.github`, ...) are not served.
- Files are read from the classpath into memory (with ETags); the server never writes to the file system, so it runs
  fine from a read-only file system (e.g. a Docker container).

## Logging

The server logs through SLF4J only. Standalone, `MultiCoinChartsMain` selects `log4j2-standalone.xml` (stdout only;
override with `-Dlog4j2.configurationFile=...`). When embedded, the host application's `log4j2.xml` applies.

## Run

```bash
gradle :MultiCoinChartsServer:run
# or
gradle :MultiCoinChartsServer:shadowJar
java -jar MultiCoinChartsServer/build/libs/MultiCoinChartsServer-1.1.8-all.jar
```

Defaults to `http://127.0.0.1:8000/`; override with `--host 0.0.0.0 --port 8080` (`--help` for usage).
Stops cleanly on Ctrl+C / SIGTERM.

## Embedding

```java
MultiCoinChartsServer server = new MultiCoinChartsServer("127.0.0.1", 8000);
server.start();   // Future<Void>
server.stop();    // Future<Void>
```

## Packaging

```bash
gradle :MultiCoinChartsServer:shadowJar       # fat jar (classifier "all")
gradle :MultiCoinChartsServer:jpackageImage   # app-image with bundled runtime, for the current OS only
```

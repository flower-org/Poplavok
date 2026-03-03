# PoplavokData - Hibernate Model

Minimal Hibernate setup for the Poplavok data layer using H2 in-memory storage.

## Quick start

```bash
gradle :PoplavokData:test
```

## Demo run

```bash
gradle :PoplavokData:run --args=""
```

## Notes

- Main entry for the demo is `com.poplavok.data.DemoApp`.
- Hibernate configuration lives in `PoplavokData/src/main/resources/hibernate.cfg.xml`.

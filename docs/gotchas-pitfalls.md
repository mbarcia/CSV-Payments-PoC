# ⚠️ Gotchas & Pitfalls

These are the rules and landmines this project learned the hard way:

## 🚫 Multi-module builds *must* run with the full Maven reactor

- Building only submodules (e.g. `-pl foo`) works for examples **but never for the framework**.
- Quarkus extensions require that runtime + deployment modules are evaluated together.
- Always use `clean install` at the root when building the framework.

## 🚫 `flatten-maven-plugin` cannot run on deployment modules

- Quarkus extensions rely on `deployment` modules containing metadata.
- Flatten strips metadata → breaking extension discovery.
- Therefore: **flatten applies only to runtime**, never deployment.

## ⚠️ Incremental builds are unsafe for Quarkus extensions

- The Quarkus augmentation phase depends on annotation processors + build steps.
- Cached class files → random or stale augmentation results.
- Always use `mvn clean` for framework changes.

## ⚠️ Testcontainers tests cannot run on PR builds

- They require built Docker images and long-lived infra.
- They cost 200–500 seconds per run.
- CI separation is mandatory.

## ⚠️ Never rely on artifact upload/download for Maven repos

- Local `~/.m2` contains dynamically generated local artifacts.
- If one artifact is missing → everything breaks (e.g. `common-1.0.jar`).
- Separate workflows → clean rebuilds.

## ⚠️ Jib builds *can* read prebuilt JARs — but you must respect module boundaries

- Running Jib at the Quarkus layer without rebuilding is fragile.
- Running Jib at the **service module** level is fine.
// Keep the root build script dependency-free so `./gradlew build` succeeds in offline/restricted environments.
// Android module is included by default; disable with: `./gradlew -PenableAndroid=false build`

tasks.register("build") {
    group = "build"
    description = "Assembles the root project. Android module is included by default; use -PenableAndroid=false to skip it."
}

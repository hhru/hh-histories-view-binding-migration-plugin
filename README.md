# hh-histories-view-binding-migration-plugin

<!-- Plugin description -->
**HH Synthetic** -- plugin for automated migration from Kotlin synthetics to View Binding.
<!-- Plugin description end -->

![Plugin usage](/docs/assets/Plugin_usage.gif)

## Try plugin
You can try download artifact from [Releases](https://github.com/hhru/hh-histories-view-binding-migration-plugin/releases),
then install it from disk into your Android Studio Arctic Fox.

### Attention!
This plugin was tested only for Android Studio Arctic Fox.

## Setup for local development

- Create `local.properties` file in root folder with the following content:

```properties
# Properties for launching Android Studio
androidStudioPath=/Users/p.strelchenko/Library/Application Support/JetBrains/Toolbox/apps/AndroidStudio/ch-0/203.7935034/Android Studio.app
androidStudioCompilerVersion=203.7717.56
```

Here:

- `androidStudioPath` - Path to your local Android Studio;
- `androidStudioCompilerVersion` - this version you could get from `About` screen of Android Studio

![Android Studio About](/docs/assets/Arctic_Fox_About.png)

package ru.hh.android.synthetic_plugin.extensions

import org.jetbrains.android.dom.manifest.cachedValueFromPrimaryManifest
import org.jetbrains.android.facet.AndroidFacet

fun AndroidFacet.getPackageName(): String? {
    return cachedValueFromPrimaryManifest { this.packageName }.value
}
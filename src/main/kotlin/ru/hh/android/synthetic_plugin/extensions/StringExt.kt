package ru.hh.android.synthetic_plugin.extensions

import android.databinding.tool.ext.toCamelCase
import ru.hh.android.synthetic_plugin.utils.Const
import ru.hh.android.synthetic_plugin.utils.Const.SET_CONTENT_VIEW_PREFIX

fun String?.isKotlinSynthetic(): Boolean {
    return this?.startsWith(Const.KOTLINX_SYNTHETIC) == true
}

/**
 * Returns in format "some_layout_name.viewName" -> "SomeLayoutNameBinding"
 */
fun String.toFormattedBindingName(): String {
    return toShortFormattedBindingName()
        .capitalize()
        .plus("Binding")
}

/**
 * Returns in format "some_layout_name.viewName" -> "someLayoutName"
 */
fun String.toShortFormattedBindingName(): String {
    val firstDot = this.indexOfFirst { it == '.' }
    val formattedName = if (firstDot != -1) {
        this.substring(0, firstDot)
    } else {
        this
    }
        .toCamelCase()
        .decapitalize()
    return formattedName
}

fun String.toMutablePropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private var _${this.decapitalize()}: $this? = null"
    } else {
        "private var _binding: $this? = null"
    }
}

fun String.toImmutablePropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private val ${this.decapitalize()}: $this get() = _${this.decapitalize()}!!"
    } else {
        "private val binding: $this get() = _binding!!"
    }
}

fun String.toDelegatePropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private val ${this.decapitalize()} by viewBindingPlugin($this::bind)"
    } else {
        "private val binding by viewBindingPlugin($this::bind)"
    }
}

fun String.toViewDelegatePropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private val ${this.decapitalize()} = inflateAndBindView($this::inflate)"
    } else {
        "private val binding = inflateAndBindView($this::inflate)"
    }
}

fun String.toViewPropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private val ${this.decapitalize()} = $this.inflate(LayoutInflater.from(context), this, false)"
    } else {
        "private val binding = $this.inflate(LayoutInflater.from(context), this, false)"
    }
}

fun String.toActivityPropertyFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "private val ${this.decapitalize()} by lazy { ${this}.inflate(layoutInflater) }"
    } else {
        "private val binding by lazy { ${this}.inflate(layoutInflater) }"
    }
}

fun String.toActivityContentViewFormat(): String {
    return "$SET_CONTENT_VIEW_PREFIX(${this}.root)"
}

/**
 * Returns in format "setContentView(R.layout.layout_name)" -> "layout_name"
 */
fun String.getLayoutNameFromContentView(): String {
    return this.substringAfterLast('.').removeSuffix(")")
}

fun String.toFragmentInitializationFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "_${this.decapitalize()} = ${this}.inflate(inflater, container, false)"
    } else {
        "_binding = ${this}.inflate(inflater, container, false)"
    }
}

fun String.toFragmentDisposingFormat(
    hasMultipleBindingsInFile: Boolean = true,
): String {
    return if (hasMultipleBindingsInFile) {
        "_${this.decapitalize()} = null"
    } else {
        "_binding = null"
    }
}
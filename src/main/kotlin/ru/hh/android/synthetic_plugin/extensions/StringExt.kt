package ru.hh.android.synthetic_plugin.extensions

import android.databinding.tool.ext.toCamelCase
import ru.hh.android.synthetic_plugin.utils.Const

fun String?.isKotlinSynthetic(): Boolean {
    return this?.startsWith(Const.KOTLINX_SYNTHETIC) == true
}

/**
 * Returns in format "some_layout_name.viewName" -> "SomeLayoutNameBinding"
 */
fun String.toFormattedBindingName(): String {
    val firstDot = this.indexOfFirst{ it == '.' }
    val formattedName = if (firstDot != -1) {
        this.substring(0, firstDot)
    } else {
        this
    }
        .toCamelCase()
        .capitalize()
        .plus("Binding")
    return formattedName
}

/**
 * Returns in format "some_layout_name.viewName" -> "someLayoutName"
 */
fun String.toShortFormattedBindingName(): String {
    val firstDot = this.indexOfFirst{ it == '.' }
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
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "private var _${this.decapitalize()}: $this? = null"
    } else {
        "private var _binding: $this? = null"
    }
}

fun String.toImmutablePropertyFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "private val ${this.decapitalize()}: $this get() = _${this.decapitalize()}"
    } else {
        "private val binding: $this get() = _binding"
    }
}

fun String.toDelegatePropertyFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "private val ${this.decapitalize()} by viewBindingPlugin($this::bind)"
    } else {
        "private val binding by viewBindingPlugin($this::bind)"
    }
}

fun String.toViewDelegatePropertyFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "private val ${this.decapitalize()} = inflateAndBindView($this::inflate)"
    } else {
        "private val binding = inflateAndBindView($this::inflate)"
    }
}

fun String.toActivityPropertyFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "private val ${this.decapitalize()} by lazy { ${this}.inflate(layoutInflater) }"
    } else {
        "private val binding by lazy { ${this}.inflate(layoutInflater) }"
    }
}

fun String.toFragmentInitializationFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "_${this.decapitalize()} = ${this}.inflate(inflater, container, false)"
    } else {
        "_binding = ${this}.inflate(inflater, container, false)"
    }
}

fun String.toFragmentDisposingFormat(
    isMultipleBindingInFile: Boolean = true,
): String {
    return if (isMultipleBindingInFile) {
        "_${this.decapitalize()} = null"
    } else {
        "_binding = null"
    }
}
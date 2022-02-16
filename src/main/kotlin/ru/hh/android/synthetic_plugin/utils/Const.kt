package ru.hh.android.synthetic_plugin.utils

object Const {
    const val ANDROID_VIEW_ID = "@+id/"
    const val KOTLINX_SYNTHETIC = "kotlinx.android.synthetic."
    const val CELL_WITH_VIEW_HOLDER = "with(viewHolder.itemView)"

    // Static length of import string "kotlinx.android.synthetic.main."
    const val CONST_SYNTHETIC_IMPORT_LENGTH = 31

    const val ON_DESTROY_FUNC_DECLARATION = "    override fun onDestroyView() {\n" +
            "        super.onDestroy()\n" +
            "    }"
}

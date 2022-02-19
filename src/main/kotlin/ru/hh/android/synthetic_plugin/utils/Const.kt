package ru.hh.android.synthetic_plugin.utils

object Const {
    const val ANDROID_VIEW_ID = "@+id/"
    const val KOTLINX_SYNTHETIC = "kotlinx.android.synthetic.main."
    const val CELL_WITH_VIEW_HOLDER = "with(viewHolder.itemView)"
    const val LAYOUT_INFLATER_PREFIX = "LayoutInflater.from"
    const val VIEW_INFLATER_PREFIX = "View.inflate"

    const val ON_DESTROY_FUNC_DECLARATION = """
    override fun onDestroyView() {
        super.onDestroy()
    }
    """
}

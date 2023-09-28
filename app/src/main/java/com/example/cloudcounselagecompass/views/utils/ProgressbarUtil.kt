package com.example.cloudcounselagecompass.views.utils

import android.app.Dialog
import android.content.Context
import androidx.annotation.LayoutRes
    object ProgressBarUtil {
        private var customProgressDialog: Dialog? = null

        fun showCustomProgressBar(context: Context, @LayoutRes layoutResId: Int) {
            if (customProgressDialog == null) {
                customProgressDialog = Dialog(context)
                customProgressDialog!!.window?.setBackgroundDrawableResource(android.R.color.transparent)
                customProgressDialog?.setContentView(layoutResId)
                customProgressDialog?.show()
            }
        }

        fun cancelCustomProgressBar() {
            if (customProgressDialog != null && customProgressDialog!!.isShowing) {
                customProgressDialog?.dismiss()
                customProgressDialog = null
            }
        }
    }


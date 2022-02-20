package ru.hh.android.synthetic_plugin.extensions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

fun Project.notifyInfo(content: String) {
    NotificationGroupManager
        .getInstance()
        .getNotificationGroup("HH Synthetic Notification Group")
        .createNotification(content, NotificationType.INFORMATION)
        .notify(this)
}
fun Project.notifyError(content: String) {
    NotificationGroupManager
        .getInstance()
        .getNotificationGroup("HH Synthetic Notification Group")
        .createNotification(content, NotificationType.ERROR)
        .notify(this)
}
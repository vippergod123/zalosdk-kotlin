package com.zing.zalo.zalosdk.core

import org.robolectric.Robolectric

object helper
{
     fun waitTaskRunInBackgroundAndForeground()
     {
          Robolectric.flushBackgroundThreadScheduler()
          Robolectric.flushForegroundThreadScheduler()
     }
}
package com.zing.zalo.zalosdk.demo

import org.robolectric.Robolectric

object helper
{
     fun waitTaskRunInBackgroundAndForeground()
     {
          Robolectric.flushBackgroundThreadScheduler()
          Robolectric.flushForegroundThreadScheduler()
     }
}
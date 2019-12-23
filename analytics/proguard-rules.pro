-verbose
-repackageclasses com.zing.zalo.zalosdk.core.analytics

-keepclasseswithmembers class com.zing.zalo.zalosdk.analytics.EventTracker { *; }
-keepclasseswithmembers class com.zing.zalo.zalosdk.analytics.EventTrackerListener { *; }
-keepclasseswithmembers class com.zing.zalo.zalosdk.analytics.IEventTracker { *; }
-keepclasseswithmembers class com.zing.zalo.zalosdk.analytics.model.Event {*;}
-keepclasseswithmembers class com.zing.zalo.zalosdk.analytics.EventTracker$Companion { com.zing.zalo.zalosdk.analytics.EventTracker getInstance(); }
#-keepclasseswithmembers interface com.zing.zalo.zalosdk.analytics.IEventTracker {*;}
#
#-keep public interface com.zing.zalo.devicetrackingsdk.ZingAnalyticsManager$CheckPreloadListener {*;}
#-keep public interface com.zing.zalo.devicetrackingsdk.abstracts.* {*;}

#
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keep public class * extends android.app.backup.BackupAgentHelper
#-keep public class * extends android.preference.Preference
#
#-keepclasseswithmembernames class * {
#    native <methods>;
#    public static <methods>;
#}
#
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet);
#}
#
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}
#
#-keepclassmembers class * extends android.app.Activity {
#    public void *(android.view.View);
#}
#
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}
#
#-keep class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}
#


#-keep public com.zing.zalo.zalosdk.analytics.model.Event {*;}
#-keep class com.zing.zalo.zalosdk.analytics.EventTracker
#
#-keep public class * extends android.content.Context
#-keep public class * extends android.content.ContentValues

# Output a source map file
-printmapping proguard.map
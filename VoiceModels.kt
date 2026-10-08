package com.example.model

data class AudioClip(
    val id: String,
    val title: String,
    val textPrompt: String,
    val filePath: String,
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isCloned: Boolean = true,
    val modelUsed: String = "gemini-3.1-flash-tts"
)

enum class RecordingStatus {
    IDLE,
    RECORDING,
    COMPLETED
}

sealed interface TtsState {
    data object Idle : TtsState
    data class Loading(val message: String) : TtsState
    data class Success(val clip: AudioClip) : TtsState
    data class Error(val message: String, val details: String? = null) : TtsState
}

data class SampleScript(
    val id: String,
    val titleMy: String,
    val titleEn: String,
    val text: String,
    val category: String
)

val DEFAULT_MYANMAR_SCRIPTS = listOf(
    SampleScript(
        id = "script_1",
        titleMy = "နေ့စဉ် နှုတ်ခွန်းဆက် (Everyday)",
        titleEn = "Friendly Conversation",
        text = "မင်္ဂလာပါခင်ဗျာ။ ကျွန်တော့်နာမည်ကတော့ စိုးနိုင် ဖြစ်ပါတယ်။ ဒီနေ့ ရာသီဥတုက သာယာပြီး လေပြည်နုအေးလေး တိုက်ခတ်နေပါတယ်။ အားလုံးပဲ ကောင်းသောနေ့လေး ဖြစ်ပါစေလို့ ဆုတောင်းပေးပါတယ်။",
        category = "Conversation"
    ),
    SampleScript(
        id = "script_2",
        titleMy = "သတင်း ကြေညာချက် (News)",
        titleEn = "Formal Broadcast",
        text = "ယနေ့ညနေ ၆ နာရီ သတင်းထုတ်ပြန်ချက်အရ ရန်ကုန်တိုင်းဒေသကြီးနှင့် မန္တလေးတိုင်းဒေသကြီးတို့တွင် အပူချိန် အနည်းငယ် မြင့်တက်နိုင်ပြီး မိုးလေဝသ အခြေအနေမှာ ကောင်းမွန်မည်ဟု သိရပါသည်။",
        category = "Formal"
    ),
    SampleScript(
        id = "script_3",
        titleMy = "ပုံပြင်နှင့် ခံစားချက် (Story)",
        titleEn = "Expressive Storytelling",
        text = "ရှေးရှေးတုန်းက သာယာလှပတဲ့ တောအုပ်ကြီးတစ်ခုထဲမှာ သစ်ပင်ကြီးတွေနဲ့ ချစ်စဖွယ် တိရစ္ဆာန်လေးတွေ နေထိုင်ကြပါတယ်။ တစ်နေ့တော့ ငှက်ကလေးတစ်ကောင်က သီချင်းချိုချိုလေးကို စတင်သီဆိုခဲ့ပါတယ်။",
        category = "Storytelling"
    )
)

val QUICK_TTS_PRESETS = listOf(
    "မင်္ဂလာပါ၊ ဒီနေ့မှာ အားလုံးပဲ စိတ်ချမ်းသာ ကိုယ်ကျန်းမာ ရှိကြပါစေ။",
    "မြန်မာ့အသံတုနည်းပညာဖြင့် သဘာဝကျသော စကားပြောသံကို နားဆင်ခံစားနိုင်ပါသည်။",
    "ကြိုးစားမှုတိုင်းမှာ အောင်မြင်မှုရလဒ်တွေ ရှိစမြဲ ဖြစ်ပါတယ်။",
    "ကျေးဇူးတင်ပါတယ်ခင်ဗျာ။ နောက်နောင်လည်း အမြဲတမ်း အဆင်သင့် ကူညီပေးပါမည်။",
    "ဉာဏ်ရည်တု အသံနည်းပညာကို စမ်းသပ်လေ့လာခြင်း ဖြစ်ပါသည်။"
)

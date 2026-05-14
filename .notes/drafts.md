ChatGPT


Tôi đang muốn làm backend cho một app. Nhờ bạn đọc các yêu cầu sau và cho tôi kế hoạch để thực hiện nhé. Tôi chưa chốt dùng tech stack nào, bạn có thể tự do quyết định. 

*Lưu ý quan trọng:*
1. Các model, kiến trúc, ngôn ngữ sử dụng, cần phải đảm bảo release mới nhất, và tra cứu từ documentation chính thức, không được dùng từ knowledge có sẵn. 
2. Nếu có chỗ nào chưa rõ ràng, hỏi lại tôi để tôi ra quyết định chứ không tự ý quyết định.

App: Đọc truyện dành cho trẻ khó đọc

Người dùng: Phụ huynh/Trẻ em. Một phụ huynh có thể giám sát nhiều trẻ em. Trẻ em cũng có thể bị giám sát bởi nhiều phụ huynh.

Yêu cầu chức năng:
1. Đăng ký/đăng nhập
- Người dùng có thể chủ động đăng ký, dưới dạng phụ huynh hoặc trẻ em. 
- Phụ huynh có thể chủ động link với trẻ em bằng cách điền email của trẻ.
- Admin có thể quản lý người dùng/phụ huynh/truyện đã có.

2. Quản lý truyện
- Admin có thể thêm/xoá/sửa truyện.
- Truyện bao gồm: Tiêu đề, nội dung, thể loại (optional), tác giả (optional).
- Phụ huynh có thể chủ động kiểm soát những truyện trẻ em có thể xem được bằng cách giới hạn không cho xem một số tựa truyện nhất định.
- Phụ huynh/trẻ em có thể search truyện theo từ khoá/filter. Trẻ em thì cần bỏ những truyện đã bị phụ huynh cấm xem.

3. Đọc truyện
- Trẻ em có thể đọc truyện từ đầu, hoặc đọc tiếp từ session đang đọc dở trước đó.
- Khi trẻ em bấm đọc => Tự động đọc từng từ một trong truyện, có ngắt nghỉ giữa các đoạn, và giọng đọc cần đồng bộ với chữ xuất hiện trên màn hình.
- Người dùng hướng đến là tiếng Việt => Cần tìm các model đọc có sẵn, có ngữ điệu (VieNeu-TTS, OmniVoice) và hỗ trợ xuất SRT để đồng bộ với chữ xuất hiện ở frontend, hoặc tìm các API providers có sẵn với chi phí free trial/hợp lý.

4. Thu âm
- Trẻ em có thể chủ động bấm thu âm để thu âm một đoạn nó đọc.
- Khi hết truyện hoặc chủ động yêu cầu dừng thu âm => Cần có cơ chế lưu lại file vào bộ nhớ trong cũng như sync với storage.
- Có thể yêu cầu Gemini đánh giá (thông qua 	API), qua mô hình gemini-3-1-flash-lite-preview (hoặc có thể switch mô hình, cái này cần cài đặt được chứ đừng hard code).

5. Tuỳ chỉnh cài đặt
- Cung cấp bộ công cụ điều chỉnh thông số hiển thị bao gồm: Phông chữ (font đặc biệt cho người khó đọc), kích thước chữ, khoảng cách dòng và khoảng cách chữ.
- Thay đổi chủ đề giao diện bao gồm màu nền và màu chữ để giảm độ chói và tăng độ tương phản.
- Hiển thị xem trước thay đổi trong thời gian thực để người giám hộ đánh giá hiệu quả trước khi áp dụng.
- Lưu trữ và áp dụng cấu hình này làm mặc định cho mọi nội dung bài đọc.
- Có thể khôi phục cấu hình mặc định ban đầu nếu cần.

6. Theo dõi tiến độ

- Tổng hợp dữ liệu từ các phiên đọc (tốc độ đọc, số lỗi, thời gian luyện tập) thành các chỉ số thống kê.
- Trình bày dữ liệu dưới dạng biểu đồ trực quan theo các mốc thời gian (ngày/tuần/tháng). Biểu đồ thể hiện xu hướng cải thiện theo thời gian.
- Lọc và trích xuất danh sách các từ khó, các từ trẻ thường xuyên đọc sai hoặc yêu cầu hỗ trợ TTS nhiều lần.
- Cung cấp chi tiết từng phiên đọc cụ thể bao gồm cả việc nghe lại các bản ghi âm tương ứng.

7. Thông báo luyện tập
- Cho phép người giám hộ thiết lập lịch trình nhắc nhở (giờ, ngày trong tuần).
- Tự động gửi thông báo đẩy đến thiết bị của trẻ đúng thời gian đã thiết lập.
- Cung cấp cơ chế "Deep Link" -- cho phép trẻ chạm vào thông báo để mở thẳng ứng dụng vào không gian luyện đọc mà không qua các bước trung gian.
- Quản lý trạng thái phản hồi của thông báo (đúng giờ, muộn giờ, đã đọc, đã thực hiện, hoặc bỏ qua).

Yêu cầu trả về: Tech stack đề xuất, các yêu cầu chức năng (bao gồm đầy đủ input, output, cách hoạt động, như một SRS).

Mình sẽ tra cứu theo nguồn chính thức trước để tránh dùng thông tin cũ: backend framework, auth/storage/notification, Gemini API, và các lựa chọn TTS tiếng Việt. Sau đó mình sẽ chốt một stack đề xuất kèm các điểm cần bạn quyết định.

Thought for 50s
Dưới đây là bản kế hoạch/SRS ở mức backend. Mình có tra cứu các nguồn chính thức mới trước khi đề xuất. Điểm rất quan trọng: model bạn nêu gemini-3.1-flash-lite-preview đã được Google thông báo deprecate ngày 11/05/2026 và shutdown ngày 25/05/2026, nên backend không nên hard-code model preview này; nên để cấu hình mặc định là gemini-3.1-flash-lite hoặc model GA mới hơn khi bạn deploy. 

1. Tech stack đề xuất
Stack chính cho MVP production
Thành phần	Đề xuất	Lý do
Backend API	Java 25 LTS + Spring Boot 4.0.6	Java 25 là LTS; Spring Boot hiện hiển thị 4.0.6 trên trang chính thức. Phù hợp app nhiều domain, auth, job, scheduler, transaction. 
Auth	Firebase Authentication + Spring Security Resource Server	Mobile app có login email/password, social login, reset password; backend verify JWT. Firebase Auth hỗ trợ email/password, reset password, nhiều provider; Spring Security Resource Server verify JWT và rotate key qua JWKS. 
Database	PostgreSQL 18.x	Quan hệ nhiều-nhiều phụ huynh–trẻ, truy vấn thống kê, search/filter, audit tốt. PostgreSQL 18.4 là bản current docs tại thời điểm tra cứu. 
Migration	Flyway	Version-control schema, chạy migration tự động khi deploy. 
Object storage	S3-compatible storage: MinIO local/dev, AWS S3/GCS/R2 production	Lưu audio recording, TTS audio, SRT/WebVTT, avatar. MinIO là object store S3-compatible. 
Queue/job	Redis Streams hoặc PostgreSQL-backed job cho MVP	Xử lý bất đồng bộ: synthesize TTS, upload audio, Gemini evaluation, notification scheduling. Redis Streams dùng cho real-time/event processing. 
Push notification	Firebase Cloud Messaging	Cross-platform Android/iOS/web, hỗ trợ payload nhỏ để truyền deep link. FCM docs ghi payload message tối đa 4096 bytes. 
AI đánh giá thu âm	Gemini API, model configurable	Gemini hỗ trợ audio input, transcription, phân tích segment và timestamp; dùng Files API cho audio dài. 
TTS	Ưu tiên Azure Speech hoặc Google Cloud TTS/Gemini TTS cho MVP; VieNeu/OmniVoice là nhánh self-host thử nghiệm	Azure có voice/language support chính thức và word-boundary event; Google Cloud TTS có SSML timepoints bằng <mark>; Gemini TTS/Gemini Cloud TTS có điều khiển style/pace/tone nhưng cần kiểm tra timestamp/word alignment trước khi chọn làm nguồn sync chính. 
Chốt stack mình khuyên dùng: Spring Boot 4 + PostgreSQL + Firebase Auth/FCM + S3-compatible storage + Azure Speech cho TTS sync chữ + Gemini API cho đánh giá thu âm.

Lý do chọn Azure Speech cho đọc từng từ đồng bộ chữ: yêu cầu của bạn không chỉ là “đọc hay”, mà là “audio đồng bộ với chữ xuất hiện”. Azure Speech SDK có WordBoundary event, hợp với việc tạo word_timings/WebVTT/SRT. Google Cloud TTS có SSML <mark> timepoints, dùng được nhưng phải chèn mark theo token/đoạn; Gemini TTS hiện mạnh về điều khiển giọng, style, tone, nhưng từ docs tra được mình chưa thấy cam kết word-level timestamps/SRT trực tiếp. 

2. Các điểm cần bạn chốt trước khi thiết kế DB/API cuối cùng
Trẻ em có tự đăng ký bằng email thật không, hay phụ huynh tạo hồ sơ con? Nếu app dành cho trẻ nhỏ, mình nghiêng về “phụ huynh tạo child profile”, còn child login bằng mã/pin hoặc email riêng là tùy độ tuổi.

Link phụ huynh–trẻ có cần consent hai chiều không? Hiện bạn nói phụ huynh điền email trẻ để link. Mình đề xuất phải có lời mời/link request, trẻ hoặc phụ huynh hiện tại của trẻ xác nhận, tránh ai biết email cũng giám sát được.

Một trẻ bị nhiều phụ huynh giám sát thì quyền cấm truyện xử lý thế nào? Có 2 rule: chỉ cần 1 phụ huynh cấm là không được xem, hoặc mỗi phụ huynh chỉ quản lý trong “family group” của mình. MVP nên dùng “any guardian bans => child cannot view”.

TTS cần chạy realtime hay pre-generate? Nếu truyện cố định, nên pre-generate audio + timing khi admin thêm truyện để đọc mượt hơn. Nếu nội dung động, mới generate on-demand.

Đánh giá đọc sai cần chính xác mức nào? Gemini có thể đánh giá tổng thể và trích lỗi, nhưng nếu muốn chấm phát âm từng từ đáng tin cậy thì nên thêm ASR/forced alignment chuyên dụng sau MVP.

App hướng thị trường nào? Vì trẻ em + recording là dữ liệu nhạy cảm. Nếu có thị trường Mỹ/EU/VN thì cần review pháp lý COPPA/GDPR/VN PDP. COPPA áp dụng cho dịch vụ hướng tới trẻ dưới 13 tuổi hoặc biết rõ đang thu thập dữ liệu trẻ dưới 13. 

3. Kiến trúc tổng quan
Mobile App
  ├─ Firebase Auth login
  ├─ Read story screen: fetch story + audio + word timings
  ├─ Recording upload
  └─ FCM notification + deep link

Backend Spring Boot
  ├─ Auth/RBAC module
  ├─ User/Guardian/Child module
  ├─ Story module
  ├─ Reading session module
  ├─ TTS orchestration module
  ├─ Recording module
  ├─ Gemini evaluation module
  ├─ Settings/accessibility module
  ├─ Progress analytics module
  └─ Notification scheduler module

PostgreSQL
  ├─ users, child_profiles, guardian_child_links
  ├─ stories, story_blocks, story_access_blocks
  ├─ reading_sessions, reading_events
  ├─ recordings, ai_evaluations
  ├─ display_settings
  └─ reminder_schedules, notification_events

Object Storage
  ├─ recordings/*.webm|m4a
  ├─ tts-audio/*.mp3|wav
  └─ tts-timings/*.json|vtt|srt

External Services
  ├─ Firebase Auth
  ├─ Firebase Cloud Messaging
  ├─ Azure/Google/Gemini TTS
  └─ Gemini API
4. Data model chính
User / Role
users

Field	Type	Ghi chú
id	uuid	internal id
firebase_uid	text unique	lấy từ Firebase token
email	text unique	nullable nếu child profile không có email
display_name	text	tên hiển thị
role	enum	ADMIN, GUARDIAN, CHILD
status	enum	ACTIVE, DISABLED, PENDING
created_at, updated_at	timestamp	audit
guardian_child_links

Field	Type	Ghi chú
guardian_id	uuid	FK users
child_id	uuid	FK users/child profile
status	enum	PENDING, ACCEPTED, REJECTED, REVOKED
invited_by	uuid	ai tạo link
created_at, accepted_at	timestamp	audit
Quan hệ phụ huynh–trẻ là many-to-many.

Story
stories

Field	Type
id	uuid
title	text
normalized_title	text
content	text
genre	text nullable
author	text nullable
difficulty_level	nullable
status	DRAFT, PUBLISHED, ARCHIVED
created_by	admin id
created_at, updated_at	timestamp
story_blocks

Dùng để chia truyện thành đoạn/câu/từ cho TTS và tracking.

Field	Type
id	uuid
story_id	uuid
block_index	int
text	text
type	PARAGRAPH, SENTENCE
start_char, end_char	int
story_tts_assets

Field	Type
id	uuid
story_id	uuid
provider	AZURE, GOOGLE_CLOUD_TTS, GEMINI_TTS, SELF_HOSTED
voice_id	text
audio_object_key	text
timing_object_key	text
timing_format	JSON, SRT, WEBVTT
status	PENDING, READY, FAILED
story_access_blocks

Field	Type
child_id	uuid
story_id	uuid
blocked_by_guardian_id	uuid
reason	text nullable
created_at	timestamp
Rule MVP: khi child search/list story, exclude story nếu tồn tại block active của bất kỳ guardian nào đang link accepted với child.

Reading session
reading_sessions

Field	Type
id	uuid
child_id	uuid
story_id	uuid
status	IN_PROGRESS, COMPLETED, ABANDONED
current_block_index	int
current_word_index	int
started_at, last_active_at, completed_at	timestamp
reading_events

Field	Type
id	uuid
session_id	uuid
event_type	START, PAUSE, RESUME, WORD_SHOWN, TTS_HELP, RECORD_START, RECORD_STOP, COMPLETE
word	text nullable
word_index	int nullable
timestamp_ms	bigint
metadata	jsonb
Recording / Evaluation
recordings

Field	Type
id	uuid
session_id	uuid
child_id	uuid
story_id	uuid
object_key_local	text nullable
object_key_remote	text
duration_ms	int
mime_type	text
upload_status	LOCAL_PENDING, UPLOADED, FAILED
created_at	timestamp
ai_evaluations

Field	Type
id	uuid
recording_id	uuid
provider	GEMINI
model_name	text
prompt_version	text
status	PENDING, DONE, FAILED
transcript	text
score_json	jsonb
difficult_words	jsonb
error_words	jsonb
created_at	timestamp
Model Gemini để trong config/database, ví dụ:

ai_model_configs

Field	Type
task	READING_EVALUATION
provider	GEMINI
model_name	text
enabled	boolean
temperature	numeric
response_schema	jsonb
Không hard-code model trong code.

5. SRS chức năng
5.1 Đăng ký / đăng nhập / phân quyền
UC-01: Đăng ký tài khoản
Actor: Phụ huynh, trẻ em, admin tạo hộ.

Input:

{
  "email": "child@example.com",
  "password": "********",
  "displayName": "Bé An",
  "role": "CHILD"
}
Output:

{
  "userId": "uuid",
  "role": "CHILD",
  "status": "ACTIVE"
}
Cách hoạt động:

Frontend đăng ký qua Firebase Auth. Sau đó gọi backend POST /api/users/bootstrap kèm Firebase ID token. Backend verify JWT, tạo record users, gán role ban đầu. Role admin không cho user tự chọn qua API public; admin phải được set bằng custom claim hoặc thao tác nội bộ. Firebase Auth hỗ trợ email/password, reset password; Firebase custom claims có thể dùng cho quyền admin/role ở token. 

UC-02: Login
Input: Firebase ID token từ client.

Output: thông tin profile backend.

{
  "userId": "uuid",
  "email": "parent@example.com",
  "role": "GUARDIAN",
  "displayName": "Mẹ An"
}
Rule:

Mọi API protected yêu cầu Authorization: Bearer <firebase_id_token>. Backend verify token, map firebase_uid sang user nội bộ.

UC-03: Phụ huynh link với trẻ
Input:

{
  "childEmail": "child@example.com"
}
Output:

{
  "linkId": "uuid",
  "status": "PENDING"
}
Cách hoạt động đề xuất:

Phụ huynh nhập email trẻ. Backend tìm user role CHILD, tạo guardian_child_links status PENDING. Trẻ hoặc phụ huynh hiện tại của trẻ xác nhận. Sau khi accepted, phụ huynh xem được progress, recording, cài đặt, chặn truyện.

Không nên: nhập email là link ngay, vì rủi ro giám sát sai trẻ.

5.2 Quản lý truyện
UC-04: Admin thêm truyện
Input:

{
  "title": "Chú mèo đi học",
  "content": "Ngày xưa có một chú mèo...",
  "genre": "Thiếu nhi",
  "author": "Nguyễn A",
  "status": "PUBLISHED"
}
Output:

{
  "storyId": "uuid",
  "ttsStatus": "PENDING"
}
Cách hoạt động:

Backend validate title/content, normalize text, tách đoạn/câu/từ, lưu stories + story_blocks. Nếu story published, tạo job GENERATE_TTS_ASSET. Job gọi TTS provider, lưu audio + timing vào object storage.

UC-05: Admin sửa/xoá truyện
Input sửa:

{
  "title": "Tên mới",
  "content": "Nội dung mới",
  "genre": "Truyện ngắn",
  "author": null,
  "status": "PUBLISHED"
}
Output:

{
  "storyId": "uuid",
  "version": 3,
  "ttsStatus": "PENDING"
}
Rule:

Nếu sửa content thì invalidate TTS cũ, tạo TTS asset mới. Không hard delete nếu đã có reading session; dùng ARCHIVED.

UC-06: Search/filter truyện
Input query:

GET /api/stories?keyword=meo&genre=Thiếu%20nhi&page=0&size=20
Output:

{
  "items": [
    {
      "storyId": "uuid",
      "title": "Chú mèo đi học",
      "genre": "Thiếu nhi",
      "author": "Nguyễn A",
      "isBlockedForCurrentChild": false
    }
  ],
  "page": 0,
  "total": 120
}
Rule:

Admin/phụ huynh có thể thấy toàn bộ theo quyền. Child chỉ thấy PUBLISHED và không bị chặn bởi guardian accepted.

UC-07: Phụ huynh chặn truyện cho trẻ
Input:

{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": true,
  "reason": "Không phù hợp độ tuổi"
}
Output:

{
  "childId": "uuid",
  "storyId": "uuid",
  "blocked": true
}
Rule:

Chỉ guardian đã link accepted với child mới được block/unblock.

5.3 Đọc truyện + TTS sync chữ
UC-08: Trẻ bắt đầu đọc truyện
Input:

{
  "storyId": "uuid",
  "mode": "START_FROM_BEGINNING"
}
Output:

{
  "sessionId": "uuid",
  "story": {
    "storyId": "uuid",
    "title": "Chú mèo đi học",
    "content": "..."
  },
  "tts": {
    "audioUrl": "signed-url",
    "timingUrl": "signed-url",
    "timingFormat": "WEBVTT"
  },
  "resumePosition": {
    "blockIndex": 0,
    "wordIndex": 0
  }
}
Cách hoạt động:

Backend kiểm tra child có quyền xem story không. Nếu chưa có session dở, tạo session mới. Nếu TTS asset đã ready, trả signed URL audio + timing. Nếu chưa ready, trả ttsStatus=PENDING để frontend fallback sang đọc text hoặc chờ.

UC-09: Đọc tiếp session đang dở
Input:

GET /api/reading-sessions/active?storyId=uuid
Output:

{
  "sessionId": "uuid",
  "status": "IN_PROGRESS",
  "currentBlockIndex": 5,
  "currentWordIndex": 37,
  "audioUrl": "signed-url",
  "timingUrl": "signed-url"
}
Cách hoạt động:

Frontend dùng timing file để highlight từng từ theo audio. Backend chỉ lưu checkpoint định kỳ, ví dụ mỗi 5–10 giây hoặc khi pause/exit.

UC-10: Update reading progress
Input:

{
  "sessionId": "uuid",
  "currentBlockIndex": 6,
  "currentWordIndex": 12,
  "elapsedMs": 94000,
  "events": [
    {
      "type": "TTS_HELP",
      "word": "khó",
      "wordIndex": 125,
      "timestampMs": 92300
    }
  ]
}
Output:

{
  "saved": true,
  "serverTime": "2026-05-25T10:00:00Z"
}
Rule:

Dữ liệu event dùng để tính tốc độ đọc, từ khó, số lần cần hỗ trợ TTS.

5.4 Thu âm + đánh giá Gemini
UC-11: Tạo recording upload session
Input:

{
  "sessionId": "uuid",
  "mimeType": "audio/webm",
  "durationEstimateMs": 120000
}
Output:

{
  "recordingId": "uuid",
  "uploadUrl": "signed-upload-url",
  "objectKey": "recordings/child/session/recording.webm"
}
Cách hoạt động:

Frontend thu âm local. Khi dừng hoặc hết truyện, frontend upload lên signed URL. Backend lưu metadata và tạo job đánh giá.

UC-12: Hoàn tất upload recording
Input:

{
  "recordingId": "uuid",
  "durationMs": 118234,
  "fileSize": 1839201
}
Output:

{
  "recordingId": "uuid",
  "uploadStatus": "UPLOADED",
  "evaluationStatus": "PENDING"
}
UC-13: Gemini đánh giá bài đọc
Input nội bộ job:

{
  "recordingId": "uuid",
  "storyText": "Ngày xưa có một chú mèo...",
  "model": "gemini-3.1-flash-lite"
}
Output lưu DB:

{
  "transcript": "Ngày xưa có một chú mèo...",
  "summary": "Đọc tương đối rõ, ngắt nghỉ tốt.",
  "scores": {
    "fluency": 0.82,
    "accuracy": 0.76,
    "pace": 0.68
  },
  "errors": [
    {
      "expected": "chú",
      "heard": "chu",
      "type": "pronunciation",
      "confidence": 0.71
    }
  ],
  "difficultWords": ["chú", "trường", "nghỉ"]
}
Cách hoạt động:

Backend upload audio cho Gemini qua Files API hoặc inline nếu nhỏ. Prompt yêu cầu JSON schema cố định. Gemini docs nói có thể phân tích audio, transcription, segment và timestamp, nhưng kết quả chấm đọc trẻ em vẫn cần benchmark thực tế trước khi dùng làm đánh giá chính thức. 

5.5 Tuỳ chỉnh hiển thị cho trẻ khó đọc
UC-14: Lấy cấu hình đọc mặc định
Input:

GET /api/children/{childId}/display-settings
Output:

{
  "fontFamily": "OpenDyslexic",
  "fontSize": 22,
  "lineHeight": 1.8,
  "letterSpacing": 0.08,
  "backgroundColor": "#FAF6E8",
  "textColor": "#1F2937",
  "themeName": "warm-low-glare"
}
UC-15: Preview cấu hình
Preview nên làm ở frontend realtime, backend không cần lưu.

Input frontend local: font, size, spacing, color.

Output: render thử đoạn mẫu.

UC-16: Lưu cấu hình
Input:

{
  "fontFamily": "OpenDyslexic",
  "fontSize": 24,
  "lineHeight": 1.9,
  "letterSpacing": 0.1,
  "backgroundColor": "#FFF7D6",
  "textColor": "#111827"
}
Output:

{
  "saved": true,
  "settingsVersion": 4
}
UC-17: Reset mặc định
Input:

POST /api/children/{childId}/display-settings/reset
Output:

{
  "fontFamily": "system",
  "fontSize": 20,
  "lineHeight": 1.6,
  "letterSpacing": 0.04,
  "backgroundColor": "#FFFFFF",
  "textColor": "#111111"
}
5.6 Theo dõi tiến độ
UC-18: Dashboard tổng quan
Input:

GET /api/children/{childId}/progress/summary?range=month
Output:

{
  "totalPracticeMinutes": 320,
  "sessionsCount": 24,
  "averageReadingSpeedWpm": 58,
  "averageErrorsPerSession": 7.2,
  "trend": {
    "readingSpeed": "+12%",
    "errors": "-18%"
  }
}
UC-19: Biểu đồ theo ngày/tuần/tháng
Input:

GET /api/children/{childId}/progress/timeseries?metric=wpm&bucket=week&from=2026-04-01&to=2026-05-25
Output:

{
  "metric": "wpm",
  "bucket": "week",
  "points": [
    {
      "period": "2026-W18",
      "value": 45
    },
    {
      "period": "2026-W19",
      "value": 52
    }
  ]
}
UC-20: Danh sách từ khó
Input:

GET /api/children/{childId}/progress/difficult-words?limit=50
Output:

{
  "items": [
    {
      "word": "trường",
      "errorCount": 8,
      "ttsHelpCount": 12,
      "lastSeenAt": "2026-05-24T09:00:00Z"
    }
  ]
}
UC-21: Chi tiết từng session
Input:

GET /api/reading-sessions/{sessionId}
Output:

{
  "sessionId": "uuid",
  "storyTitle": "Chú mèo đi học",
  "startedAt": "2026-05-25T09:00:00Z",
  "durationMs": 420000,
  "recordings": [
    {
      "recordingId": "uuid",
      "audioUrl": "signed-url",
      "evaluationStatus": "DONE"
    }
  ],
  "metrics": {
    "wpm": 55,
    "errors": 6,
    "ttsHelpCount": 4
  },
  "evaluation": {
    "summary": "Đọc rõ hơn so với tuần trước...",
    "difficultWords": ["trường", "nghỉ"]
  }
}
5.7 Thông báo luyện tập + deep link
UC-22: Phụ huynh tạo lịch nhắc
Input:

{
  "childId": "uuid",
  "daysOfWeek": ["MONDAY", "WEDNESDAY", "FRIDAY"],
  "time": "19:30",
  "timezone": "Asia/Ho_Chi_Minh",
  "message": "Đến giờ luyện đọc rồi con nhé!"
}
Output:

{
  "scheduleId": "uuid",
  "enabled": true
}
UC-23: Gửi push notification
Job input nội bộ:

{
  "scheduleId": "uuid",
  "childId": "uuid",
  "deviceToken": "fcm-token",
  "deepLink": "app://reading/practice?childId=uuid"
}
FCM payload đề xuất:

{
  "notification": {
    "title": "Luyện đọc",
    "body": "Đến giờ luyện đọc rồi con nhé!"
  },
  "data": {
    "type": "PRACTICE_REMINDER",
    "deepLink": "app://reading/practice?childId=uuid",
    "notificationEventId": "uuid"
  }
}
FCM phù hợp vì là dịch vụ cross-platform để gửi notification; payload cần nhỏ vì giới hạn message payload 4096 bytes. 

UC-24: Track trạng thái phản hồi thông báo
Input từ app:

{
  "notificationEventId": "uuid",
  "status": "OPENED_LATE",
  "openedAt": "2026-05-25T19:45:00+07:00"
}
Output:

{
  "saved": true
}
Status đề xuất:

SCHEDULED, SENT, DELIVERED_IF_AVAILABLE, OPENED_ON_TIME, OPENED_LATE, PRACTICE_STARTED, IGNORED, FAILED.

Lưu ý: “delivered” chính xác tuyệt đối không phải lúc nào cũng có từ FCM/app; backend nên coi các trạng thái do app report là nguồn chính cho OPENED và PRACTICE_STARTED.

6. API module đề xuất
/auth
  POST /api/users/bootstrap
  GET  /api/me

/admin/users
  GET  /api/admin/users
  PATCH /api/admin/users/{id}/status

/guardian-child-links
  POST /api/guardian-child-links
  POST /api/guardian-child-links/{id}/accept
  POST /api/guardian-child-links/{id}/reject
  DELETE /api/guardian-child-links/{id}

/stories
  GET /api/stories
  GET /api/stories/{id}
  POST /api/admin/stories
  PATCH /api/admin/stories/{id}
  DELETE /api/admin/stories/{id}

/story-access
  POST /api/story-access/block
  POST /api/story-access/unblock

/reading-sessions
  POST /api/reading-sessions
  GET /api/reading-sessions/active
  PATCH /api/reading-sessions/{id}/progress
  POST /api/reading-sessions/{id}/complete

/recordings
  POST /api/recordings/upload-session
  POST /api/recordings/{id}/complete-upload
  GET /api/recordings/{id}

/display-settings
  GET /api/children/{childId}/display-settings
  PUT /api/children/{childId}/display-settings
  POST /api/children/{childId}/display-settings/reset

/progress
  GET /api/children/{childId}/progress/summary
  GET /api/children/{childId}/progress/timeseries
  GET /api/children/{childId}/progress/difficult-words
  GET /api/reading-sessions/{sessionId}

/reminders
  POST /api/reminders
  GET /api/children/{childId}/reminders
  PATCH /api/reminders/{id}
  DELETE /api/reminders/{id}
  POST /api/notifications/{id}/status
7. Luồng nghiệp vụ quan trọng
Luồng thêm truyện
Admin tạo truyện.

Backend tách nội dung thành block/sentence/word.

Backend tạo job TTS.

TTS service sinh audio.

Timing service sinh word_timings.

Backend lưu audio + timing vào object storage.

Story chuyển sang ttsStatus=READY.

Luồng đọc truyện
Child mở story.

Backend check quyền xem.

Backend tạo hoặc resume session.

Frontend tải audio + timing.

Frontend highlight chữ theo timing.

Frontend gửi checkpoint và events định kỳ.

Khi hoàn thành, backend tính session metrics sơ bộ.

Luồng thu âm và đánh giá
Child bấm thu.

App lưu file local trong lúc đọc.

Khi dừng, app xin signed upload URL.

Upload object storage.

Backend tạo evaluation job.

Job gửi audio + story text sang Gemini.

Gemini trả JSON đánh giá.

Backend lưu transcript, score, lỗi, từ khó.

Dashboard cập nhật.

Luồng nhắc luyện tập
Guardian tạo lịch.

Scheduler tìm reminder đến hạn theo timezone.

Backend gửi FCM data message có deep link.

Child tap notification.

App mở thẳng màn luyện đọc.

App report OPENED_ON_TIME, OPENED_LATE, hoặc PRACTICE_STARTED.

8. Non-functional requirements
Bảo mật
Vì app xử lý trẻ em và audio, cần tối thiểu:

Không public URL audio; dùng signed URL có hạn.

Mã hoá at-rest nếu dùng cloud storage.

Role-based access control: child chỉ xem dữ liệu của mình, guardian chỉ xem child đã link accepted, admin có audit.

Audit log cho: link child, block story, xem recording, xoá recording.

Không gửi recording cho AI provider nếu chưa có consent phù hợp.

Data retention: ví dụ xoá recording sau 90/180 ngày hoặc theo cài đặt phụ huynh.

Privacy/legal
Nếu app hướng tới trẻ dưới 13 ở Mỹ, COPPA yêu cầu tuân thủ khi dịch vụ hướng đến trẻ em hoặc biết đang thu thập thông tin cá nhân trẻ dưới 13. Với Việt Nam/EU cũng cần kiểm tra luật dữ liệu cá nhân trước khi production. Đây không phải tư vấn pháp lý, nhưng về kỹ thuật nên thiết kế sẵn consent, retention, export/delete data. 

Hiệu năng
Story list/search: phân trang, index title, genre, status.

Search tiếng Việt: dùng PostgreSQL full-text/trigram hoặc sau này Elasticsearch/OpenSearch nếu dữ liệu lớn.

TTS: pre-generate cho truyện published.

Recording upload: dùng signed URL, không stream qua backend nếu file lớn.

Progress dashboard: aggregate bảng ngày/tuần để tránh query raw events quá nhiều.

9. Roadmap triển khai
Phase 1 — MVP lõi
Auth Firebase + user role.

Guardian-child link.

CRUD story admin.

Search story + block story.

Reading session + resume.

Display settings.

Upload recording.

Gemini evaluation async.

Basic progress summary.

Phase 2 — TTS sync chữ
Tách story thành token/word.

Tích hợp Azure Speech/Google TTS.

Sinh word_timings.json + WebVTT/SRT.

Frontend highlight theo timing.

Cache audio/timing theo story version + voice.

Phase 3 — Notification
Device token management.

Reminder schedule.

FCM push.

Deep link.

Notification status tracking.

Phase 4 — Analytics nâng cao
Trend theo ngày/tuần/tháng.

Difficult words ranking.

Session replay + audio playback.

So sánh tiến bộ theo từng truyện/từng loại từ.

Export report cho phụ huynh.

10. Quyết định mình cần bạn chốt
Mình sẽ không tự ý chốt các điểm này vì ảnh hưởng trực tiếp thiết kế:

Child login bằng email/password riêng hay phụ huynh tạo child profile + PIN?

Link guardian-child có cần child/current guardian accept không?

Một phụ huynh cấm truyện thì có cấm cho child toàn cục không?

Bạn muốn TTS cloud API trước, hay chấp nhận self-host VieNeu/OmniVoice ngay từ đầu?

Recording giữ bao lâu: 30/90/180 ngày hay vĩnh viễn?

App mobile dùng React Native/Flutter/native? Việc deep link và FCM payload sẽ phụ thuộc frontend stack.


Không bắt buộc Firebase. Mình sẽ tách riêng auth và push notification, vì auth self-host được khá ổn, còn push mobile thì có phần không thể self-host 100% do iOS/Android phụ thuộc APNs/FCM.

Thought for 12s
Không bắt buộc Firebase. Nhưng phải tách làm 2 phần:

Auth: self-host được khá ổn.
Push notification mobile: không self-host 100% được nếu muốn app nhận thông báo khi app đang tắt/background.
1. Auth có self-host được không?
Có. Bạn có thể bỏ Firebase Auth và dùng self-host identity provider.

Các lựa chọn hợp lý:

Option	Nhận xét
Keycloak	Mạnh, open-source, self-host, hỗ trợ user management, OAuth2/OIDC, role, SSO. Hơi nặng nhưng chuẩn bài backend production. Keycloak tự mô tả là open-source IAM, có user federation, strong authentication, user management, fine-grained authorization. 
Supabase Auth self-host	Nếu bạn muốn auth + Postgres + storage chung ecosystem. Supabase docs có self-host bằng Docker và config Auth qua docker-compose.yml. 
Auth.js / custom JWT	Làm được, nhưng với mobile + trẻ em + guardian-child permission, mình không khuyên tự viết toàn bộ auth nếu chưa có kinh nghiệm security.
authentik	Open-source/self-host identity provider, nhưng thường hợp internal app/homelab hơn app mobile consumer. 
Với app của bạn, nếu muốn self-host thì mình đề xuất:

Keycloak + Spring Security OAuth2 Resource Server
Flow:

React Native app
→ login qua Keycloak
→ nhận access_token JWT
→ gọi Spring Boot API với Bearer token
→ Spring verify JWT qua Keycloak JWKS
Backend vẫn có bảng users riêng để lưu domain profile:

users
- id
- keycloak_user_id
- email
- role: ADMIN / GUARDIAN / CHILD
- status
Keycloak lo login/password/reset password/email verification. Spring Boot lo nghiệp vụ app.

2. Push notification có self-host được không?
Một nửa thôi. Bạn có thể tự host scheduler, notification database, template, deep link, trạng thái notification. Nhưng để điện thoại nhận push khi app đang tắt thì vẫn phải đi qua hệ thống của OS:

iOS → bắt buộc qua APNs của Apple
Android → thường qua FCM của Google
Apple docs nói APNs phải biết device address trước khi gửi notification, và server của bạn sẽ gửi notification request tới APNs. 
 Firebase Cloud Messaging thì cung cấp kết nối server-device cho iOS/Android/web. 

Nói cách khác:

Bạn self-host backend notification được.
Nhưng đường push xuống thiết bị thì vẫn phải qua APNs/FCM.
3. Có bỏ Firebase Cloud Messaging được không?
Android
Có thể tự làm một channel realtime bằng WebSocket/SSE/MQTT, nhưng nó chỉ ổn khi app đang foreground hoặc có foreground service. Khi app bị kill/background lâu, Android sẽ hạn chế background process để tiết kiệm pin. Muốn push chuẩn production thì gần như vẫn dùng FCM.

iOS
Gần như không có lựa chọn self-host thay APNs cho remote push. iOS remote notification đi qua APNs.

Vậy nếu bạn không muốn Firebase, hướng đúng là:

iOS: backend gọi APNs trực tiếp
Android: backend gọi FCM trực tiếp
Tức là không dùng Firebase Auth, nhưng vẫn có thể phải dùng FCM cho Android push.

4. Vậy nên chọn gì cho app sinh viên?
Mình đề xuất như này:

Auth:
- Self-host Keycloak

Push:
- MVP đơn giản: dùng FCM cho Android/iOS qua React Native Firebase Messaging
- Nếu muốn bớt phụ thuộc Firebase: gọi APNs trực tiếp cho iOS, FCM trực tiếp cho Android
Vì bạn dùng React Native, @react-native-firebase/messaging vẫn là cách dễ nhất để lấy device token và nhận push; docs của React Native Firebase ghi FCM là service cost-free cho server-device/device-device messaging. 

5. Kiến trúc cập nhật nếu không dùng Firebase Auth
React Native App
  ├─ Login với Keycloak
  ├─ Nhận JWT access token
  ├─ Gửi API request tới Spring Boot
  └─ Đăng ký FCM/APNs device token

Spring Boot Backend
  ├─ Verify JWT từ Keycloak
  ├─ RBAC: ADMIN / GUARDIAN / CHILD
  ├─ Story / Reading / Recording / Progress
  ├─ Reminder scheduler
  └─ Push sender

Keycloak
  ├─ User login
  ├─ Email verification
  ├─ Reset password
  └─ Token issuing

Push layer
  ├─ FCM for Android
  └─ APNs for iOS, hoặc FCM bridge cho iOS
6. Có nên tự viết auth không?
Với app này: không nên.

Bạn đang có:

- trẻ em login
- phụ huynh link child
- nhiều guardian quản lý một child
- admin
- dữ liệu audio
- quyền xem recording/progress
Nếu tự viết auth, dễ dính lỗi password hashing, reset password token, refresh token, revoke token, email verification, session/device management. Dùng Keycloak sẽ an toàn hơn nhiều.

7. Kết luận
Không bắt buộc Firebase.

Mình sẽ sửa stack thành:

Auth:
  Keycloak self-host

Backend:
  Spring Boot verify JWT từ Keycloak

Push:
  Không thể self-host 100% cho mobile remote push
  Dùng FCM/APNs ở tầng delivery
  Backend notification scheduler vẫn self-host

React Native:
  Login qua Keycloak
  Nhận push token qua FCM/APNs library
  Deep link mở màn luyện đọc
Nói gọn: Auth self-host được. Push notification mobile thì backend tự host được, nhưng delivery tới máy vẫn phải qua APNs/FCM.


Supabase self host hơi căng, tại tôi self host có tí mà nó đi kèm cả: storage, auth, kong, postgrest, imgproxy ... rất nặng máy

Đúng. Supabase self-host hơi quá tay cho app này, nhất là máy dev sinh viên. Nó tiện nếu bạn muốn cả “BaaS ecosystem”, nhưng nếu backend chính đã là Spring Boot thì kéo thêm kong, postgrest, storage, imgproxy, realtime, auth… sẽ nặng và cũng bị trùng trách nhiệm với backend của mình.

Mình sẽ bỏ Supabase khỏi stack đề xuất.

Stack nhẹ hơn nên dùng
Backend chính:
- Spring Boot
- PostgreSQL
- Flyway
- MinIO hoặc local filesystem cho object storage dev
- Redis optional, chưa cần ngay

Auth:
- Keycloak nếu muốn chuẩn OIDC/self-host
- Hoặc Spring Authorization Server nếu muốn all-in Java nhưng setup auth sẽ khó hơn
- Không dùng Supabase Auth

AI/TTS:
- Python service riêng
- VieNeu-TTS
- Montreal Forced Aligner Vietnamese

Push:
- Backend tự quản reminder/schedule/status
- Delivery mobile vẫn qua FCM/APNs
Với máy dev, mình khuyên chạy tối thiểu như này
docker-compose dev:
- postgres
- keycloak
- minio
- spring-backend
- python-ai-service

Không chạy:
- supabase-kong
- postgrest
- realtime
- imgproxy
- supabase-storage-api
Nếu máy vẫn nặng, có thể giảm tiếp:

Dev ultra-light:
- postgres bằng Docker
- keycloak bằng Docker
- spring-backend chạy local
- python-ai-service chạy local
- storage dùng thư mục local trước, sau mới đổi MinIO
Keycloak có nặng không?
Có, nhưng nó đúng việc hơn Supabase. Keycloak chỉ lo identity/auth:

login
reset password
email verification
OIDC/JWT
role/claim
Nó không kéo theo cả BaaS stack. Với app này, Keycloak hợp hơn Supabase self-host.

Nếu muốn còn nhẹ hơn Keycloak?
Có 2 hướng:

Hướng 1: Dùng Spring Boot tự làm auth tối giản
Dùng:

Spring Security
BCrypt/Argon2 password hash
JWT access token
refresh token table
email verification token
password reset token
Ưu điểm:

ít service nhất
dev nhẹ
dễ deploy demo
Nhược điểm:

tự chịu trách nhiệm security
phải làm refresh/revoke/reset/email verify cẩn thận
khó mở rộng social login/SSO
Với đồ án/MVP sinh viên, hướng này chấp nhận được nếu scope nhỏ và làm kỹ.

Hướng 2: Keycloak cho auth chuẩn
Ưu điểm:

chuẩn OIDC
ít tự viết security nhạy cảm
sau này production đỡ đau
Nhược điểm:

thêm 1 service
React Native login flow phức tạp hơn custom auth một chút
Mình chọn gì cho bạn?
Nếu mục tiêu là đồ án/MVP chạy được nhanh, máy nhẹ, mình chọn:

Spring Boot custom auth trước
PostgreSQL
MinIO optional
Python AI service
Nhưng phải thiết kế sao cho sau này đổi sang Keycloak được:

users.external_auth_provider nullable
users.external_auth_id nullable
Ban đầu:

auth_provider = LOCAL
Sau này:

auth_provider = KEYCLOAK
Auth local tối thiểu nên có
Bảng users:

id
email unique
password_hash
role: ADMIN / GUARDIAN / CHILD
display_name
email_verified
status: ACTIVE / DISABLED
created_at
updated_at
Bảng refresh_tokens:

id
user_id
token_hash
expires_at
revoked_at
device_id
created_at
Bảng email_verification_tokens:

id
user_id
token_hash
expires_at
used_at
Bảng password_reset_tokens:

id
user_id
token_hash
expires_at
used_at
Login output:

{
  "accessToken": "jwt...",
  "refreshToken": "opaque-random-token",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "email": "child@example.com",
    "role": "CHILD"
  }
}
Access token nên ngắn hạn, ví dụ 15 phút. Refresh token lưu hash trong DB, revoke được khi logout.

Kết luận
Bạn nghĩ đúng: Supabase self-host không hợp trong case này vì quá nặng so với phần mình cần.

Mình sẽ sửa đề xuất thành một trong hai mode:

Mode nhẹ cho MVP sinh viên:
- Spring Boot custom auth
- PostgreSQL
- Local filesystem/MinIO
- Python AI service

Mode chuẩn hơn nếu production:
- Keycloak
- Spring Boot Resource Server
- PostgreSQL
- MinIO/S3
- Python AI service
Còn push notification thì vẫn không tránh được FCM/APNs nếu muốn thông báo hiện khi app tắt/background, nhưng phần schedule/status/deep link vẫn tự host ở backend được.


auth ko dùng qua provider mà tự cài, tự hash pass, tự sign token dc ko

Được, nhưng phải làm đúng mức “tự cài auth” chứ không phải chỉ hash password + sign JWT là xong. Mình sẽ nêu bộ tối thiểu cần có để không tự đào hố security.

Thought for 6s
Được. Với app sinh viên/MVP, tự cài auth trong Spring Boot là phương án hợp lý, miễn là bạn làm đủ các phần tối thiểu. Không cần Firebase Auth, Keycloak, Supabase Auth.

Nhưng đừng hiểu auth là chỉ:

hash password + sign JWT
Mà nên là:

register/login
+ hash password đúng cách
+ access token ngắn hạn
+ refresh token revoke được
+ email verification nếu cần
+ reset password
+ logout
+ role/permission
+ rate limit login
+ audit log cơ bản
OWASP khuyến nghị password phải dùng thuật toán hash chậm, có salt như Argon2id/bcrypt/PBKDF2; không dùng SHA-256/MD5 để hash password vì quá nhanh để brute-force. 

1. Stack auth tự cài nên dùng
Với Spring Boot:

Spring Security
Spring Web
PostgreSQL
Flyway
jjwt hoặc nimbus-jose-jwt để sign/verify JWT
BCryptPasswordEncoder hoặc Argon2PasswordEncoder
Với Java/Spring, lựa chọn thực dụng nhất:

Password hash: BCrypt
Access token: JWT, hết hạn ngắn
Refresh token: opaque random token, lưu hash trong DB
Mình không khuyên refresh token cũng là JWT nếu bạn muốn logout/revoke đơn giản.

2. Flow chuẩn
Register
Client gửi email + password + role
→ backend validate
→ hash password
→ tạo user status ACTIVE hoặc PENDING_EMAIL_VERIFY
→ trả user info
Password không bao giờ lưu plain text. BCrypt/Argon2 tự sinh salt trong hash string.

Login
Client gửi email + password
→ backend tìm user
→ passwordEncoder.matches(rawPassword, passwordHash)
→ nếu đúng:
    tạo access JWT 15 phút
    tạo refresh token random 256-bit
    hash refresh token rồi lưu DB
→ trả token pair
Output:

{
  "accessToken": "jwt...",
  "refreshToken": "random-opaque-token",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "email": "child@example.com",
    "role": "CHILD"
  }
}
Refresh
Client gửi refreshToken
→ backend hash token này
→ tìm trong refresh_tokens
→ check chưa expired, chưa revoked
→ rotate refresh token:
    revoke token cũ
    tạo token mới
→ trả access token mới + refresh token mới
Nên rotate refresh token để nếu token bị lộ thì giảm thiệt hại.

Logout
Client gửi refreshToken
→ backend revoke refresh token trong DB
→ access token cũ vẫn có thể sống đến hết 15 phút
Muốn logout “ngay lập tức” thì phải có blacklist access token, nhưng MVP thường không cần nếu access token chỉ 10–15 phút.

3. Database tối thiểu
users
create table users (
    id uuid primary key,
    email text not null unique,
    password_hash text not null,
    role text not null check (role in ('ADMIN', 'GUARDIAN', 'CHILD')),
    display_name text not null,
    email_verified boolean not null default false,
    status text not null check (status in ('ACTIVE', 'DISABLED', 'PENDING_EMAIL_VERIFY')),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);
refresh_tokens
create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash text not null unique,
    device_id text,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now()
);
password_reset_tokens
create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash text not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);
email_verification_tokens
create table email_verification_tokens (
    id uuid primary key,
    user_id uuid not null references users(id),
    token_hash text not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null default now()
);
4. JWT nên chứa gì?
Access token payload nên nhỏ thôi:

{
  "sub": "user-uuid",
  "role": "CHILD",
  "iat": 1710000000,
  "exp": 1710000900,
  "jti": "token-id"
}
Không nhét thông tin nhạy cảm vào JWT. JWT chỉ được ký, không mặc định được mã hoá. OWASP cũng cảnh báo JWT là nguồn lỗi bảo mật phổ biến nếu implement sai, nên phải kiểm tra signature, expiry, algorithm, claims cẩn thận. 

5. Access token lưu ở đâu trong React Native?
Với React Native:

access token: memory hoặc secure storage
refresh token: secure storage
Dùng:

react-native-keychain
hoặc expo-secure-store nếu dùng Expo
Không lưu token trong AsyncStorage nếu có thể tránh.

6. Những lỗi không nên mắc
Không nên:

hash password bằng SHA-256
lưu refresh token plain text trong DB
access token sống 7 ngày
không có revoke/logout
không rate limit login
JWT dùng alg=none hoặc không check alg
đưa role từ request body rồi tin luôn
cho user tự đăng ký role ADMIN
trả lỗi “email tồn tại/sai password” quá chi tiết ở login
Nên làm:

BCrypt hoặc Argon2id
refresh token random, lưu hash
access token 10–15 phút
refresh token 7–30 ngày
rotate refresh token
rate limit login/register/reset password
admin role seed bằng migration hoặc env bootstrap
7. Role cho app của bạn
Bạn có 3 role chính:

ADMIN
GUARDIAN
CHILD
Nhưng permission thực tế không chỉ dựa vào role. Ví dụ guardian muốn xem progress child thì phải check thêm:

guardian_child_links.status = ACCEPTED
Tức là:

Role check: người này có phải GUARDIAN không?
Relation check: người này có đang quản lý child này không?
Ví dụ API:

GET /api/children/{childId}/progress
Backend phải check:

currentUser.role == GUARDIAN
AND exists guardian_child_links
    where guardian_id = currentUser.id
    and child_id = childId
    and status = ACCEPTED
8. Kết luận
Làm được, và với app của bạn mình thấy custom auth trong Spring Boot là hợp lý cho MVP.

Stack chốt:

Spring Boot custom auth
PostgreSQL
BCrypt/Argon2id password hash
JWT access token 15 phút
Opaque refresh token lưu hash trong DB
Role + relation-based authorization
Câu quan trọng nhất: tự sign JWT được, nhưng refresh token phải quản lý trong DB để logout/revoke/rotate được.



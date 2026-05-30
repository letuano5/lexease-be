# Open Questions

Các điểm này chưa nên tự quyết khi implement chi tiết.

## Auth

1. Admin đầu tiên seed bằng cách nào?
   - Option A: env `APP_BOOTSTRAP_ADMIN_EMAIL` + password ở local/dev.
   - Option B: SQL seed riêng cho local.
   - Option C: CLI/internal endpoint chỉ bật ở profile local.
   - Decision: cung cấp SQL script để promote một user đã register lên `ADMIN`; không bootstrap admin bằng app startup.

2. Refresh token TTL là bao lâu?
   - Đề xuất MVP: access token 15 phút, refresh token 30 ngày.
   - Decision: 30 ngày.

3. Child có được tự đăng ký bằng email/password không?
   - Draft nói có.
   - Nếu trẻ nhỏ, có thể cần guardian tạo hộ child account.
   - Decision: có, child được public register bằng email/password.

## Guardian-Child Link

4. Link guardian-child cần ai accept?
   - Option A: child accept nếu child đã có account.
   - Option B: nếu child đã có guardian, một current guardian accept.
   - Option C: cả child và current guardian accept.
   - Decision: chỉ cần một guardian hiện tại accept là đủ khi cần guardian approval.

5. Một child có nhiều guardian, một guardian block story thì child bị block toàn cục hay chỉ trong context guardian đó?
   - Đề xuất MVP: block toàn cục cho child nếu blocker là accepted guardian.
   - Decision: block toàn cục cho child.

## Story

6. Author chỉ là text array hay cần entity author riêng?
   - Đề xuất MVP: text array qua bảng `story_authors`.
   - Decision: dùng entity riêng `authors`; story liên kết qua join table `story_authors`. `genres` cũng là entity riêng tương tự.

7. Có cần difficulty level cho story ngay từ đầu không?
   - Draft có nhắc optional trong data model cũ, nhưng yêu cầu phase 2 chưa bắt buộc.

## Display Settings

8. Child có được tự chỉnh display settings không?
   - Option A: child chỉ đọc settings.
   - Option B: child được chỉnh settings của mình.
   - Option C: guardian/admin mới được chỉnh.
   - Decision: child được tự đọc và tự chỉnh settings của chính mình.

9. Font allowlist gồm những font nào?
   - Cần frontend chốt vì backend chỉ lưu key/tên font.
   - Decision: tạm thời chưa có allowlist; backend chỉ validate `fontFamily` nonblank + max length.

## Notifications

10. iOS push dùng FCM bridge hay gọi APNs trực tiếp?
    - Android gần như vẫn dùng FCM.
    - Decision: dùng Firebase Cloud Messaging cho MVP. iOS đi qua Firebase Messaging/APNs bridge, chưa gọi APNs trực tiếp.

11. Thế nào là `OPENED_ON_TIME` và `OPENED_LATE`?
    - Ví dụ: on time nếu mở trong 15 phút sau scheduled time.
    - Decision: backend tính `OPENED_ON_TIME` nếu mở trong `NOTIFICATIONS_OPENED_ON_TIME_WINDOW`, mặc định 15 phút sau `scheduled_for`; quá cửa sổ này là `OPENED_LATE`.

12. Nếu nhiều thiết bị của cùng child, gửi push đến tất cả hay thiết bị active gần nhất?
    - Decision: gửi đến tất cả active device tokens của child.

## TTS / MFA

13. VieNeu chạy như service riêng bằng Python hay batch script được Spring gọi?
    - Đề xuất: Python service riêng.

14. Object storage dev dùng local filesystem hay MinIO ngay từ đầu?
    - Đề xuất: filesystem local trước, giữ interface để đổi MinIO.

15. Có cần nhiều voice/speed cho cùng một story không?
    - Nếu có, `tts_assets` phải key theo `voiceId` và có thể thêm `speechRate`.

16. Khi TTS/MFA fail, child có được đọc text-only không?
    - Đề xuất: có, trả `tts.status = PENDING/FAILED`.

## Gemini / Recording

17. Recording retention bao lâu?
    - Bạn từng nói 30 ngày trong draft chat.
    - Cần chốt: 30/90/180 ngày hay giữ đến khi guardian xoá.

18. Có cần consent riêng trước khi gửi recording của child lên Gemini không?

19. Gemini model mặc định dùng model nào?
    - Không hard-code. Cần chốt default config khi implement.

20. Scoring có cần so sánh từng từ với timing hay chỉ chấm tổng thể + lỗi nổi bật?
    - Nếu cần từng từ rất chính xác, Gemini đơn lẻ có thể chưa đủ; sẽ cần ASR/forced alignment riêng cho recording.

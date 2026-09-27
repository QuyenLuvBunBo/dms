# Hướng dẫn từng bước — Claude Code + Claude Design cho DMS

Để file này trong `docs/WORKFLOW.md`. Prompt để nguyên tiếng Anh vì codebase và giao diện đều tiếng Anh.

---

## Bước 0 — Chuẩn bị (một người làm, một lần)

**Cài trên máy chạy Claude Code:** JDK 21, Node.js 20+, Docker Desktop (bắt buộc — test chạy MySQL thật qua Testcontainers), Git, Claude Code.

**Tạo repo:**

1. Tạo repo GitHub trống tên `dms`, add cả nhóm làm collaborator, clone về máy.
2. Đặt file vào đúng chỗ:

```
dms/
├── CLAUDE.md                 ← file mình gửi
└── docs/
    ├── BUILD_PLAN.md         ← file mình gửi
    ├── PROGRESS.md           ← file mình gửi
    └── WORKFLOW.md           ← file này
```

3. Commit và push: `git add . && git commit -m "Project docs" && git push`
4. Mở terminal ở thư mục `dms`, gõ `claude`.
5. Gõ `/memory` để kiểm tra Claude đã nạp `CLAUDE.md`. **Không chạy `/init`** — nó dùng để sinh `CLAUDE.md` mới, mình đã có rồi.

---

## Vòng lặp chuẩn cho mỗi phase

Mọi phase từ 0 đến 6 đều đi đúng 6 bước này. Các mục sau chỉ ghi những gì khác biệt.

**① Tạo nhánh**

```bash
git checkout main && git pull
git checkout -b phase-1        # thay số theo phase đang làm: phase-0, phase-1, ...
```

Trong mọi prompt bên dưới, **N là số phase** — thay bằng số thật trước khi gửi.

**② Xóa ngữ cảnh cũ:** gõ `/clear` trong Claude Code. Phase trước đã ghi vào `PROGRESS.md` nên không mất gì.

**③ Lập kế hoạch**

```
/plan Read CLAUDE.md, docs/BUILD_PLAN.md and docs/PROGRESS.md.
Plan Phase N. Include:
1. The business-rule tests you will write first, with their @DisplayName
2. Every file you will create or change
3. Any ambiguity in the spec that the team must decide
Do not plan anything outside Phase N.
```

**④ Duyệt kế hoạch:** nhấn `Ctrl+G` (hoặc gõ `/plan open`) để mở file kế hoạch. Đọc kỹ, ghi chú thẳng vào file những chỗ không đồng ý, lưu lại. Nếu nó nêu điểm mơ hồ, trả lời trong chat. Chỉ khi ưng mới chấp nhận để nó bắt đầu làm.

Mỗi phase có mục "Kiểm tra kế hoạch" riêng bên dưới — đó là những chỗ hay sai nhất.

**⑤ Để nó làm, rồi tự kiểm tra lại**

```bash
docker compose up -d
cd backend && ./mvnw test
```

Tự chạy thử app và đi qua mục **"Done when"** của phase đó trong `BUILD_PLAN.md`. Đừng tin chỉ vì nó báo "all tests pass".

**⑥ Chốt phase**

```
Phase N is done. Please:
1. Append a summary to docs/PROGRESS.md: what was built, which BR tests pass,
   decisions made, and open questions for the team
2. Copy the approved plan into docs/plans/phase-N.md
3. Commit everything with the message "Phase N: <short summary>"
```

Rồi `git push -u origin phase-N`, mở Pull Request trên GitHub, **một người khác trong nhóm review** rồi mới merge vào `main`.

---

## Phase 0 — Setup và phân quyền ✅ xong

Kèm nâng cấp lên Spring Boot 4.1.

---

## Phase 1 — Tòa nhà, loại phòng, quản lý tòa

Chạy vòng lặp chuẩn với N = 1.

**Kiểm tra kế hoạch — phải có đủ:**
- Migration thêm role `BUILDING_MANAGER`, bỏ `AFFAIRS`, cập nhật tài khoản seed
- Thay các settings seed của Phase 0 bằng bảng settings mới trong `CLAUDE.md`
- Loại phòng gắn theo tòa, có sức chứa, diện tích, tiện ích, giá thuê tháng
- BR-14 được test trên **mọi** endpoint của phần facility, và trả về 404 chứ không phải 403

**Tự kiểm tra:** đăng nhập quản lý B6, thử mở một phòng của B9 bằng URL → phải ra 404.

**Sau khi xong:** chụp màn hình danh sách phòng và chi tiết phòng cho Claude Design.

---

## Claude Design — làm song song với Phase 1

Bộ màn hình cũ vẽ theo mô hình chấm điểm và admin xếp giường, **không dùng nữa**. Trong project Claude Design đang có, gửi prompt làm lại ở dưới.

**Gửi kèm:** `CLAUDE.md` và `docs/BUILD_PLAN.md` mới từ repo, file thông báo KTX K71 (PDF), và ảnh chụp màn hình Phase 1 nếu đã có.

**Prompt 1 — Làm lại Phase 2–3:**

```
The process changed after our field study. Discard the Phase 2–3 screens
(scoring, admin review, bed assignment). Use the attached CLAUDE.md,
BUILD_PLAN.md and the official HUST dormitory notice. Keep the existing
visual style, sidebar and status badge conventions.

Student (desktop and mobile):
1. Rounds list: each round with my priority group's opening time and a
   countdown; locked until my window opens
2. Declaration: personal, academic, address and family details, priority
   group, photo upload, proof upload (required for UT1/UT2)
3. Registration wizard in 4 steps like the current portal: confirm details
   → choose building (free beds per building) → choose room (free beds,
   room type, monthly rent, amenities, room gender, floor preference as a
   default filter) → confirm with the semester fee breakdown
4. Hold screen: 30-minute countdown, simulated QR payment, fee breakdown
   (rent × months, water × months, equipment fee)
5. States: window not open yet, bed just taken by someone else (409),
   hold expired, room full
6. My residence: bed, stay period, residence history, roommates

Building manager:
7. Check-in: search student, photo and declaration, verify or reject
   priority proof, check in button disabled until proof is verified
   for UT1/UT2

Centre administrator:
8. Round setup: type, stay period, hold minutes, buildings offered, and
   one opening time per priority group (UT1, UT2, UT3)

Use realistic Vietnamese student names, buildings B6 and B9, and the real
prices: 6-student room 1,050,000, 8-student 730,000, 10-student 550,000
VND per month, water 40,000, equipment 300,000.
```

**Prompt 2 — Trước Phase 4:**

```
Design the electricity screens in the same style:
1. Building manager: end-of-month meter reading grid for a whole building,
   with a warning when a reading is lower than last month
2. Student: my electricity share for a month — room consumption, price per
   kWh, my days, total occupant-days, my share, Pay button; and the case of
   a mid-month transfer with two rooms
```

**Prompt 3 — Trước Phase 5:**

```
Design in the same style:
1. Student: report a fault, my tickets with status and due date,
   building announcements feed
2. Building manager: ticket queue sorted by due date with overdue
   highlighted, assign technician, publish announcement, record violation
3. Technician: my assigned tickets with status actions
```

**Bàn giao:** **Export → Handoff to Claude Code → Send to local coding agent**, lưu prompt để dán vào `/plan` của phase tương ứng.

---

## Phase 2 — Đợt đăng ký, khai báo, chọn phòng, giữ chỗ ⚠️ phase khó nhất

Vòng lặp chuẩn, **bước ③ thay bằng:**

```
/plan Read CLAUDE.md, docs/BUILD_PLAN.md and docs/PROGRESS.md.
Plan Phase 2. Also read the design handoff below and use it ONLY for the
Phase 2 screens (rounds list, declaration, registration wizard, hold
screen and its states, round setup). Include the BR tests you will write
first, every file you will create or change, and any ambiguity.

<dán prompt handoff từ Claude Design vào đây>
```

**Kiểm tra kế hoạch — soi kỹ:**
- BR-04 có **2 test**: 2 thread giành 1 giường (đúng 1 thành công), và 50 thread giành 10 giường (đúng 10 lượt giữ, không giường nào bị giữ 2 lần). Chạy trên MySQL thật, không `Thread.sleep`
- BR-05 dùng test clock để tua qua 30 phút; hóa đơn phải chuyển `VOID`, không bị xóa
- BR-12 có đủ 3 bộ số: phòng 8 người 4.920.000, phòng 10 người 3.840.000, phòng 6 người 6.840.000 — và trường hợp vào ở ngày cuối tháng vẫn tính cả tháng
- BR-03: ưu tiên tầng theo giới tính **chỉ là bộ lọc mặc định**, không được chặn lựa chọn
- File ảnh và minh chứng lưu ngoài database, và có test sinh viên này không tải được ảnh của sinh viên khác

**Tự kiểm tra:** mở 2 trình duyệt (1 thường, 1 ẩn danh), đăng nhập 2 sinh viên, cùng bấm giữ một giường cuối cùng.

---

## Phase 3 — Thanh toán, nhận phòng, ở lại, chuyển phòng

Vòng lặp chuẩn, dùng handoff cho màn check-in và My residence.

**Kiểm tra kế hoạch:**
- Thanh toán sau khi hết giờ giữ bị từ chối với 409
- BR-15: nút check-in bị khóa khi minh chứng UT1/UT2 chưa xác minh — và backend cũng chặn, không chỉ ẩn nút
- BR-13: trong đợt ở lại, giường của người đang ở không hiện cho ai khác
- File CSV khai báo lưu trú chỉ gồm cư dân của tòa mình quản lý (BR-14)

**Tự kiểm tra:** đi hết luồng khai báo → giữ chỗ → thanh toán → check-in → chuyển phòng → đăng ký ở lại.

---

## Phase 4 — Tiền điện ⚠️

Vòng lặp chuẩn, dùng handoff từ Prompt 2.

**Kiểm tra kế hoạch — test BR-07 phải có đủ 5 trường hợp:**
1. Ở cả tháng
2. Vào ở giữa tháng
3. Chuyển phòng giữa tháng (ra 2 hóa đơn điện, mỗi phòng một hóa đơn)
4. Số lẻ khi làm tròn được cộng cho người ở nhiều ngày nhất
5. Tổng các phần **bằng đúng** tiền điện cả phòng

**Tự kiểm tra bằng tay:** lấy 1 phòng, tự tính trên giấy (số điện × 3.000đ, chia theo ngày), so với hóa đơn hệ thống sinh ra.

---

## Phase 5 — Sửa chữa, thông báo, vi phạm

Vòng lặp chuẩn, dùng handoff từ Prompt 3.

**Kiểm tra kế hoạch:** kỹ thuật viên chỉ thấy phiếu giao cho mình; cư dân B6 không bao giờ thấy thông báo của B9; đủ ngưỡng cảnh báo chỉ tạo yêu cầu xem xét, **không** tự động làm gì khác (BR-11).

---

## Phase 6 — Báo cáo và hoàn thiện

Trước khi chạy: vẽ các dashboard trên Claude Design, handoff như trên.

**Kiểm tra kế hoạch:** có script mô phỏng mở cửa sổ đăng ký với vài trăm sinh viên giữ chỗ cùng lúc, và kiểm tra sau đó không giường nào bị giữ 2 lần.

**Tự kiểm tra:** xóa sạch DB, chạy với profile `demo`, mọi dashboard có số liệu có nghĩa. `./mvnw test` — toàn bộ BR-01 đến BR-15 phải pass.

---

## Khi có sự cố

**Test fail mãi, nó sửa lung tung:**

```
Stop. Do not change any code yet. Explain why <tên test> fails,
what you think the root cause is, and what you propose to change.
```

**Nó định tắt, xóa hoặc sửa test cho pass:** từ chối, và nhắc:

```
Do not disable, delete or weaken tests. CLAUDE.md forbids it.
Fix the implementation instead, or tell me if the test itself is wrong and why.
```

**Nó làm tính năng ngoài phạm vi:** `That is out of scope in BUILD_PLAN.md. Revert it.`

**Phiên chat quá dài, nó bắt đầu quên quy tắc:** chốt phase dở dang bằng bước ⑥ (ghi rõ phần chưa xong vào PROGRESS.md), `/clear`, rồi `/plan` tiếp từ chỗ đó.

---

## Tóm tắt: gửi file nào ở đâu

| Ở đâu | Gửi gì |
|---|---|
| Repo (Claude Code tự đọc) | `CLAUDE.md` ở gốc; `docs/BUILD_PLAN.md`, `docs/PROGRESS.md` |
| Claude Design | Đính kèm `CLAUDE.md` + `BUILD_PLAN.md` từ repo, thông báo KTX K71 (PDF); ảnh chụp màn hình app khi đã có |
| Claude Code, bước `/plan` các phase có giao diện | Dán prompt handoff từ Claude Design |
| Form nộp đề tài | `DMS_Project_Proposal_Group7.docx` — **không** cần đưa vào repo |
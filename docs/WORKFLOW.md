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
git checkout -b phase-N
```

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

## Phase 0 — Setup và phân quyền

Chạy vòng lặp chuẩn với N = 0.

**Kiểm tra kế hoạch — phải có đủ:**
- Testcontainers MySQL, **không có H2**
- Bean `Clock` inject được, và test clock đổi được thời gian
- Flyway migration, **không có** `ddl-auto=update`
- `@RestControllerAdvice` trả `ProblemDetail`
- Seed 1 tài khoản cho mỗi role

**Tự kiểm tra:** đăng nhập lần lượt 5 role, mỗi role chỉ thấy menu của mình. Đăng nhập student rồi thử gọi một URL admin → phải bị 403.

---

## Phase 1 — Tòa nhà, phòng, giường

Chạy vòng lặp chuẩn với N = 1.

**Kiểm tra kế hoạch:** phòng có người ở thì không xóa được; số giường khớp sức chứa phòng.

**Sau khi xong:** chụp vài ảnh màn hình trang danh sách phòng và chi tiết phòng — dùng cho Claude Design ở bước sau.

---

## Claude Design — người phụ trách UI làm song song với Phase 0–1

**Mở ở đâu:** claude.ai → chat mới (hoặc claude.ai/design).

**Gửi kèm file:** `CLAUDE.md` và `docs/BUILD_PLAN.md` (để nó biết các trạng thái và dữ liệu thật). Nếu Phase 1 đã xong thì gửi thêm ảnh chụp màn hình để giữ phong cách nhất quán.

**Prompt 1 — Cổng sinh viên:**

```
Using the attached CLAUDE.md and BUILD_PLAN.md, design the student portal
for a university Dormitory Management System. English UI, mobile-first,
clean and minimal. Screens:
1. Dashboard: current room and bed, contract status, unpaid invoices,
   open repair tickets
2. Application: form, then a status page showing the priority score
   breakdown by criterion
3. Invoice detail: rent line, utility line with the full calculation
   (room total, my days in room, total occupant-days, my share),
   adjustment lines, Pay button
4. Repair tickets: report a fault, list with status and due date
Use realistic sample data with Vietnamese student names and VND amounts.
```

**Prompt 2 — Phía admin:**

```
Now design the admin side, desktop-first, same visual style:
1. Admission round setup: quota, deadline, and a weights editor for the
   four scoring criteria
2. Application review: table sorted by priority score, expandable score
   breakdown, Approve / Reject / Waitlist actions
3. Bed assignment board: grid of rooms per floor, each room showing
   gender lock, occupied and free beds; select an approved applicant
   then click a free bed to assign
Show me 2–3 layout options for the bed assignment board.
```

**Prompt 3 — Kế toán và kỹ thuật viên:**

```
Design the remaining roles, same style:
1. Accountant: monthly meter reading entry grid per room (with a warning
   when a reading is lower than last month), invoice generation for a
   month, payment list
2. Technician: ticket queue sorted by due date, overdue tickets highlighted,
   ticket detail with status actions
```

**Chỉnh sửa:** sửa bố cục lớn thì nói trong chat; sửa một nút, một khoảng cách thì click thẳng lên canvas để comment. Chốt bố cục trên giấy trong buổi họp nhóm trước khi vẽ — mỗi lần vẽ lại đều ăn vào quota dùng chung với Claude Code.

**Bàn giao:** bấm **Export → Handoff to Claude Code → Send to local coding agent**. Nó đưa ra một prompt có chứa URL của gói thiết kế — **copy lại prompt này**, dùng ở Phase 2.

---

## Phase 2 — Đợt đăng ký và đơn

Vòng lặp chuẩn, nhưng **bước ③ thay bằng:**

```
/plan Read CLAUDE.md, docs/BUILD_PLAN.md and docs/PROGRESS.md.
Plan Phase 2. Also read the design handoff below and use it ONLY for the
screens that belong to Phase 2 (application form, application status,
admission round setup, application review). Ignore the other screens for now.
Include the BR tests you will write first, every file you will create or
change, and any ambiguity.

<dán nguyên prompt handoff từ Claude Design vào đây>
```

**Kiểm tra kế hoạch:** trọng số chấm điểm đọc từ `scoring_config` của đợt, không nằm trong code (BR-01); có test chứng minh đổi trọng số thì thứ hạng đổi.

**Sau khi merge:** trong Claude Code gõ `/design-sync` và làm theo hướng dẫn để kéo component của frontend về Claude Design. Các màn vẽ sau sẽ dùng đúng component đã có.

---

## Phase 3 — Xếp giường và hợp đồng ⚠️ phase khó nhất

Vòng lặp chuẩn. Ở bước ③ thêm dòng: `Use the design handoff for the bed assignment board and the student dashboard.` và dán lại prompt handoff.

**Kiểm tra kế hoạch — soi kỹ:**
- BR-04: test phải chạy **2 thread thật** (ExecutorService + CountDownLatch) trên MySQL, và khẳng định **đúng 1** thành công
- BR-03: có test cả khóa lẫn **mở khóa** giới tính khi người cuối rời phòng
- BR-05: dùng test clock để tua qua hạn offer, không dùng `Thread.sleep`
- Chuyển phòng: đóng dòng `residence_histories` cũ, mở dòng mới, **cùng một hợp đồng**

**Tự kiểm tra:** đi hết luồng trên UI: nộp đơn → duyệt → xếp giường → nhận → check-in → chuyển phòng → trả phòng.

---

## Phase 4 — Chỉ số điện nước và hóa đơn ⚠️

Vòng lặp chuẩn, dùng handoff cho màn hóa đơn sinh viên và màn nhập chỉ số.

**Kiểm tra kế hoạch — test BR-07 phải có đủ 5 trường hợp:**
1. Ở cả tháng
2. Vào ở giữa tháng
3. Chuyển phòng giữa tháng (ra 2 dòng tiền điện, mỗi phòng một dòng)
4. Số lẻ khi làm tròn được cộng cho người ở nhiều ngày nhất
5. Tổng các phần **bằng đúng** tiền cả phòng

Nếu thiếu, ghi chú vào kế hoạch bắt nó bổ sung trước khi làm.

**Tự kiểm tra bằng tay:** tự tính 1 phòng trên giấy, so với hóa đơn nó sinh ra.

---

## Phase 5 — Bảo trì và kỷ luật

Vòng lặp chuẩn, dùng handoff cho màn kỹ thuật viên.

**Kiểm tra kế hoạch:** kỹ thuật viên chỉ thấy phiếu giao cho mình; đề xuất đuổi khỏi KTX **không có hiệu lực** cho tới khi role AFFAIRS duyệt (BR-11).

---

## Phase 6 — Báo cáo và hoàn thiện

Trước khi chạy: quay lại Claude Design vẽ các dashboard (tỷ lệ lấp đầy, công nợ, tiêu thụ theo tháng, thời gian xử lý sửa chữa), handoff lại như trên.

Vòng lặp chuẩn. **Tự kiểm tra:** xóa sạch DB, chạy với profile `demo`, mọi dashboard phải có số liệu có nghĩa. Chạy `./mvnw test` — toàn bộ BR-01 đến BR-11 phải pass.

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
| Claude Design | Đính kèm `CLAUDE.md` + `BUILD_PLAN.md`; ảnh chụp màn hình app khi đã có |
| Claude Code, bước `/plan` các phase có giao diện | Dán prompt handoff từ Claude Design |
| Form nộp đề tài | `DMS_Project_Proposal_Group7.docx` — **không** cần đưa vào repo |

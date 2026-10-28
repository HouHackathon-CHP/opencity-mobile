# License audit — OpenCity Mobile

Kiểm tra tại thời điểm tạo repo: 32 package npm trực tiếp và 604 thư mục package đã cài (bao gồm phụ thuộc bắc cầu). Tất cả package phần mềm có SPDX identifier hoặc biểu thức được rà soát theo danh sách [giấy phép OSI](https://opensource.org/licenses). `BlueOak-1.0.0` cũng nằm trong [danh sách OSI](https://opensource.org/license/blue-oak-model-license/). Các biểu thức `OR` có ít nhất một lựa chọn đã được OSI duyệt; biểu thức `AND` chứa hai giấy phép đã được duyệt.

Ngoại lệ: `caniuse-lite` khai báo `CC-BY-4.0`. Đây là bộ dữ liệu tương thích trình duyệt, không phải giấy phép phần mềm OSI. Vì vậy không nên tuyên bố **mọi** package trong cây npm đều có license OSI. Script kiểm tra chỉ cho phép đúng ngoại lệ này theo tên package và SPDX identifier; mọi license mới đều khiến CI thất bại để được rà soát.

Giới hạn: chỉ đọc trường `license` trong `package.json`; chưa kiểm tra license của từng file, mã native tải qua Gradle, fonts/assets, dịch vụ bản đồ hoặc dữ liệu bên ngoài. Các dependency có thể thay đổi khi `package-lock.json` được cập nhật; CI chạy lại cùng script. `npm audit` tại thời điểm này còn advisory mức high trong Expo CLI/transitive `node-forge`, `zod`; cần rà soát hoặc cập nhật trước bản phát hành sản xuất.

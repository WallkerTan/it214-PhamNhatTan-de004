room-service quản lí , truy cập thông tin phòng, sử dung redis cache
notifi-service nhận kafka
ko thay dổi service, package, enpoite, cấu trúc entity, dto
ko viếc dockerfile, compose hoặc triển khai trên cloud

chức năng 
 lấy tt phòng theo id
 tạo phiếu dặt phòng
trạng thái ban đầu của phiếu đặt hàng là pending
kafka dùng cho phiếu đặt phòng là resvervation-created

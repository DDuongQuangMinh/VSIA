package com.k1ngtle.vsia.phone.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class PhoneI18n {
    private static final Map<String, Map<String, String>> EXTERNAL_PACK_CACHE =
            new HashMap<>();

    private static final Map<String, String> VIETNAMESE =
            Map.ofEntries(
            Map.entry("Changing iPhone Language translates the VS:IA phone interface immediately.", "Thay đổi Ngôn ngữ iPhone sẽ dịch giao diện điện thoại VS:IA ngay lập tức."),
            Map.entry("Region changes date formatting, time zone and default units. Temperature changes the Weather widget immediately.", "Vùng thay đổi định dạng ngày, múi giờ và đơn vị mặc định. Nhiệt độ thay đổi tiện ích Thời tiết ngay lập tức."),
            Map.entry("Add Event", "Thêm sự kiện"),
            Map.entry("Add Reminder", "Thêm lời nhắc"),
            Map.entry("All Notes", "Tất cả ghi chú"),
            Map.entry("Calendar", "Lịch"),
            Map.entry("Camera", "Camera"),
            Map.entry("Captured", "Đã chụp"),
            Map.entry("Clear Completed", "Xóa mục đã hoàn thành"),
            Map.entry("Delete", "Xóa"),
            Map.entry("Done", "Xong"),
            Map.entry("Edit Note", "Sửa ghi chú"),
            Map.entry("Event Title", "Tên sự kiện"),
            Map.entry("Events", "Sự kiện"),
            Map.entry("Front", "Trước"),
            Map.entry("New Event", "Sự kiện mới"),
            Map.entry("New Note", "Ghi chú mới"),
            Map.entry("New Reminder", "Lời nhắc mới"),
            Map.entry("No Events", "Không có sự kiện"),
            Map.entry("No Notes", "Không có ghi chú"),
            Map.entry("No Photos", "Không có ảnh"),
            Map.entry("No Reminders", "Không có lời nhắc"),
            Map.entry("Note", "Ghi chú"),
            Map.entry("Notes", "Ghi chú"),
            Map.entry("Photos", "Ảnh"),
            Map.entry("Rear", "Sau"),
            Map.entry("Reminder", "Lời nhắc"),
            Map.entry("Reminders", "Lời nhắc"),
            Map.entry("Save", "Lưu"),
            Map.entry("Shutter", "Chụp"),
            Map.entry("Stopwatch", "Bấm giờ"),
            Map.entry("Today", "Hôm nay"),
            Map.entry("Title", "Tiêu đề"),
            Map.entry("Type note...", "Nhập ghi chú..."),
            Map.entry("Type reminder...", "Nhập lời nhắc..."),
            Map.entry("24-Hour Time", "Giờ 24 giờ"),
            Map.entry("APP PRIVACY", "QUYỀN RIÊNG TƯ ỨNG DỤNG"),
            Map.entry("AUTHENTICATED", "ĐÃ XÁC THỰC"),
            Map.entry("AUTOMATIC UPDATES", "CẬP NHẬT TỰ ĐỘNG"),
            Map.entry("About", "Giới thiệu"),
            Map.entry("Accessibility", "Trợ năng"),
            Map.entry("Action Button", "Nút Tác vụ"),
            Map.entry("Aero", "Aero"),
            Map.entry("Airplane Mode", "Chế độ máy bay"),
            Map.entry("Airplane Mode · Wi-Fi available", "Chế độ máy bay · Có thể dùng Wi‑Fi"),
            Map.entry("Alert Volume", "Âm lượng cảnh báo"),
            Map.entry("All Regions", "Tất cả vùng"),
            Map.entry("Allow Messages", "Cho phép Tin nhắn"),
            Map.entry("Allow Notifications", "Cho phép thông báo"),
            Map.entry("App Store", "App Store"),
            Map.entry("Apps", "Ứng dụng"),
            Map.entry("Audio Descriptions", "Mô tả âm thanh"),
            Map.entry("Auto-Join", "Tự động kết nối"),
            Map.entry("Automatically Download", "Tự động tải về"),
            Map.entry("Automatically Install", "Tự động cài đặt"),
            Map.entry("BANNERS & HISTORY", "BIỂU NGỮ & LỊCH SỬ"),
            Map.entry("BRIGHTNESS", "ĐỘ SÁNG"),
            Map.entry("Back", "Quay lại"),
            Map.entry("Battery", "Pin"),
            Map.entry("Battery Health", "Tình trạng pin"),
            Map.entry("Battery Level", "Mức pin"),
            Map.entry("Battery must be at least 20%", "Pin phải còn ít nhất 20%"),
            Map.entry("Bluetooth", "Bluetooth"),
            Map.entry("Bluetooth is Off", "Bluetooth đang tắt"),
            Map.entry("Bluetooth stays available in Airplane Mode unless you turn it off.", "Bluetooth vẫn khả dụng trong Chế độ máy bay trừ khi bạn tắt nó."),
            Map.entry("Bold Text", "Chữ đậm"),
            Map.entry("Browser", "Trình duyệt"),
            Map.entry("Button Shapes", "Hình dạng nút"),
            Map.entry("Calendar", "Lịch"),
            Map.entry("Camera", "Camera"),
            Map.entry("Cancel", "Hủy"),
            Map.entry("Carrier", "Nhà mạng"),
            Map.entry("Cellular", "Di động"),
            Map.entry("Cellular Data", "Dữ liệu di động"),
            Map.entry("Celsius", "Độ C"),
            Map.entry("Channel", "Kênh"),
            Map.entry("Check for Update", "Kiểm tra bản cập nhật"),
            Map.entry("Checking for Update...", "Đang kiểm tra bản cập nhật..."),
            Map.entry("Choose Language", "Chọn ngôn ngữ"),
            Map.entry("Choose Region", "Chọn vùng"),
            Map.entry("Clock", "Đồng hồ"),
            Map.entry("Close", "Đóng"),
            Map.entry("Confirm", "Xác nhận"),
            Map.entry("Controls the Search pill shown above the dock.", "Điều khiển nút Tìm kiếm phía trên thanh dock."),
            Map.entry("Controls whether VS:IA Web can send requests over the simulated local network.", "Kiểm soát việc VS:IA Web có thể gửi yêu cầu qua mạng cục bộ mô phỏng hay không."),
            Map.entry("Current", "Hiện tại"),
            Map.entry("Current Position", "Vị trí hiện tại"),
            Map.entry("DNS", "DNS"),
            Map.entry("DO NOT DISTURB", "KHÔNG LÀM PHIỀN"),
            Map.entry("Data Off", "Dữ liệu tắt"),
            Map.entry("Date", "Ngày"),
            Map.entry("Date & Time", "Ngày & Giờ"),
            Map.entry("Diagnostics >", "Chẩn đoán >"),
            Map.entry("Differentiate Without Color", "Phân biệt không dùng màu"),
            Map.entry("Display", "Màn hình"),
            Map.entry("Display & Brightness", "Màn hình & Độ sáng"),
            Map.entry("Display & Text Size", "Màn hình & Cỡ chữ"),
            Map.entry("Distance", "Khoảng cách"),
            Map.entry("Do Not Disturb", "Không làm phiền"),
            Map.entry("Do Not Disturb silences Messages banners and alert sounds unless Messages is allowed. Messages still arrive.", "Không làm phiền sẽ tắt biểu ngữ và âm báo Tin nhắn trừ khi Tin nhắn được cho phép. Tin nhắn vẫn được nhận."),
            Map.entry("Done", "Xong"),
            Map.entry("Download Update", "Tải bản cập nhật"),
            Map.entry("Downloading...", "Đang tải..."),
            Map.entry("Dusk", "Hoàng hôn"),
            Map.entry("Emergency SOS", "SOS khẩn cấp"),
            Map.entry("English (UK)", "English (UK)"),
            Map.entry("English (US)", "English (US)"),
            Map.entry("Erase All Content and Settings", "Xóa tất cả nội dung và cài đặt"),
            Map.entry("Erase Local Phone Data", "Xóa dữ liệu điện thoại cục bộ"),
            Map.entry("Erase This VS:IA Phone?", "Xóa điện thoại VS:IA này?"),
            Map.entry("Error", "Lỗi"),
            Map.entry("Estimated DL", "Tốc độ tải ước tính"),
            Map.entry("Face ID & Attention", "Face ID & Sự chú ý"),
            Map.entry("Face ID & Passcode", "Face ID & Mật mã"),
            Map.entry("FaceTime", "FaceTime"),
            Map.entry("Fahrenheit", "Độ F"),
            Map.entry("Find My", "Tìm"),
            Map.entry("First Day of Week", "Ngày đầu tuần"),
            Map.entry("Focus", "Tập trung"),
            Map.entry("Focus can silence banners without blocking message delivery.", "Tập trung có thể tắt biểu ngữ mà không chặn việc nhận tin nhắn."),
            Map.entry("Forget This Network", "Quên mạng này"),
            Map.entry("Forward", "Tiến"),
            Map.entry("Friday", "Thứ Sáu"),
            Map.entry("General", "Cài đặt chung"),
            Map.entry("Graphite", "Than chì"),
            Map.entry("Haptics", "Cảm ứng"),
            Map.entry("Health", "Sức khỏe"),
            Map.entry("Home Screen & App Library", "Màn hình chính & Thư viện ứng dụng"),
            Map.entry("Hover Text", "Văn bản khi di chuột"),
            Map.entry("INACTIVE", "KHÔNG HOẠT ĐỘNG"),
            Map.entry("IP Address", "Địa chỉ IP"),
            Map.entry("Incorrect Password", "Mật khẩu không đúng"),
            Map.entry("Increase Contrast", "Tăng độ tương phản"),
            Map.entry("Install Now", "Cài đặt ngay"),
            Map.entry("Installing update...", "Đang cài đặt bản cập nhật..."),
            Map.entry("Installing...", "Đang cài đặt..."),
            Map.entry("Internet connection required", "Cần kết nối Internet"),
            Map.entry("Join", "Kết nối"),
            Map.entry("LANGUAGE", "NGÔN NGỮ"),
            Map.entry("Language", "Ngôn ngữ"),
            Map.entry("Language & Region", "Ngôn ngữ & Vùng"),
            Map.entry("Larger Text", "Chữ lớn hơn"),
            Map.entry("Learn more...", "Tìm hiểu thêm..."),
            Map.entry("Loading...", "Đang tải..."),
            Map.entry("Local Network", "Mạng cục bộ"),
            Map.entry("Local Network access is disabled.", "Quyền truy cập Mạng cục bộ đã bị tắt."),
            Map.entry("Low Power Mode", "Chế độ nguồn điện thấp"),
            Map.entry("Low Power Mode reduces background wireless refresh frequency.", "Chế độ nguồn điện thấp giảm tần suất làm mới mạng không dây nền."),
            Map.entry("MY DEVICES", "THIẾT BỊ CỦA TÔI"),
            Map.entry("Mail", "Mail"),
            Map.entry("Maps", "Bản đồ"),
            Map.entry("Measurement System", "Hệ đo lường"),
            Map.entry("Message", "Tin nhắn"),
            Map.entry("Messages", "Tin nhắn"),
            Map.entry("Messages are still delivered when notifications are off.", "Tin nhắn vẫn được nhận khi thông báo bị tắt."),
            Map.entry("Metric", "Hệ mét"),
            Map.entry("Mobile Service", "Dịch vụ di động"),
            Map.entry("Model Name", "Tên kiểu máy"),
            Map.entry("Model Number", "Số kiểu máy"),
            Map.entry("Monday", "Thứ Hai"),
            Map.entry("Motion", "Chuyển động"),
            Map.entry("NETWORK", "MẠNG"),
            Map.entry("NETWORKS", "MẠNG"),
            Map.entry("NO SERVICE", "KHÔNG CÓ DỊCH VỤ"),
            Map.entry("NOT AUTHENTICATED", "CHƯA XÁC THỰC"),
            Map.entry("Name", "Tên"),
            Map.entry("New Message", "Tin nhắn mới"),
            Map.entry("News", "Tin tức"),
            Map.entry("No Bluetooth devices registered", "Chưa đăng ký thiết bị Bluetooth"),
            Map.entry("No Internet Connection", "Không có kết nối Internet"),
            Map.entry("No SIM", "Không có SIM"),
            Map.entry("No Service", "Không có dịch vụ"),
            Map.entry("None", "Không"),
            Map.entry("Normal", "Bình thường"),
            Map.entry("Not Connected", "Chưa kết nối"),
            Map.entry("Notes", "Ghi chú"),
            Map.entry("Notification History", "Lịch sử thông báo"),
            Map.entry("Notifications", "Thông báo"),
            Map.entry("Now", "Bây giờ"),
            Map.entry("OFF", "TẮT"),
            Map.entry("Off", "Tắt"),
            Map.entry("On", "Bật"),
            Map.entry("On/Off Labels", "Nhãn Bật/Tắt"),
            Map.entry("Open", "Mở"),
            Map.entry("Other...", "Khác..."),
            Map.entry("Overworld", "Overworld"),
            Map.entry("PACKET DATA", "DỮ LIỆU GÓI"),
            Map.entry("PDU Session", "Phiên PDU"),
            Map.entry("PHYSICAL AND MOTOR", "THỂ CHẤT VÀ VẬN ĐỘNG"),
            Map.entry("Partly Cloudy", "Có mây"),
            Map.entry("Password", "Mật khẩu"),
            Map.entry("Personalize the phone for vision, mobility, hearing, speech, and cognition.", "Cá nhân hóa điện thoại cho thị giác, vận động, thính giác, lời nói và nhận thức."),
            Map.entry("PhoneOS Version", "Phiên bản PhoneOS"),
            Map.entry("PhoneOS is up to date.", "PhoneOS đã được cập nhật."),
            Map.entry("Photos", "Ảnh"),
            Map.entry("Podcasts", "Podcast"),
            Map.entry("Prefer Horizontal Text", "Ưu tiên chữ ngang"),
            Map.entry("Prepare for New iPhone", "Chuẩn bị cho iPhone mới"),
            Map.entry("Prepare for New iPhone information", "Thông tin chuẩn bị cho iPhone mới"),
            Map.entry("Preview Alert", "Xem trước cảnh báo"),
            Map.entry("Privacy & Security", "Quyền riêng tư & Bảo mật"),
            Map.entry("REGION", "VÙNG"),
            Map.entry("RINGTONE AND ALERTS", "NHẠC CHUÔNG VÀ CẢNH BÁO"),
            Map.entry("Radio", "Vô tuyến"),
            Map.entry("Radio is on", "Bluetooth đang bật"),
            Map.entry("Read & Speak", "Đọc & Nói"),
            Map.entry("Reduce Transparency", "Giảm độ trong suốt"),
            Map.entry("Region", "Vùng"),
            Map.entry("Region Time Zone", "Múi giờ theo vùng"),
            Map.entry("Region changes date formatting and default units. Temperature changes the Weather widget immediately.", "Vùng thay đổi định dạng ngày, múi giờ và đơn vị mặc định. Nhiệt độ thay đổi tiện ích Thời tiết ngay lập tức."),
            Map.entry("Reload", "Tải lại"),
            Map.entry("Reminders", "Lời nhắc"),
            Map.entry("Reset", "Đặt lại"),
            Map.entry("Reset All Settings", "Đặt lại tất cả cài đặt"),
            Map.entry("Reset All Settings?", "Đặt lại tất cả cài đặt?"),
            Map.entry("Reset Home Screen Layout", "Đặt lại bố cục Màn hình chính"),
            Map.entry("Reset Home Screen Layout?", "Đặt lại bố cục Màn hình chính?"),
            Map.entry("Reset Network Settings", "Đặt lại cài đặt mạng"),
            Map.entry("Reset Network Settings?", "Đặt lại cài đặt mạng?"),
            Map.entry("Restricted", "Bị hạn chế"),
            Map.entry("Resume", "Tiếp tục"),
            Map.entry("Router", "Bộ định tuyến"),
            Map.entry("SEARCH", "TÌM KIẾM"),
            Map.entry("SIMs", "SIM"),
            Map.entry("STOPWATCH", "BẤM GIỜ"),
            Map.entry("Saturday", "Thứ Bảy"),
            Map.entry("Screen Time", "Thời gian sử dụng"),
            Map.entry("Search", "Tìm kiếm"),
            Map.entry("Search Off", "Tìm kiếm Tắt"),
            Map.entry("Search On", "Tìm kiếm Bật"),
            Map.entry("Security", "Bảo mật"),
            Map.entry("Selected", "Đã chọn"),
            Map.entry("Send SMS", "Gửi SMS"),
            Map.entry("Serial Number", "Số sê-ri"),
            Map.entry("Settings", "Cài đặt"),
            Map.entry("Show Previews", "Hiển thị bản xem trước"),
            Map.entry("Show on Home Screen", "Hiển thị trên Màn hình chính"),
            Map.entry("Signal", "Tín hiệu"),
            Map.entry("Silent", "Im lặng"),
            Map.entry("Siri", "Siri"),
            Map.entry("Software Update", "Cập nhật phần mềm"),
            Map.entry("Sounds", "Âm thanh"),
            Map.entry("Sounds & Haptics", "Âm thanh & Cảm ứng"),
            Map.entry("Start", "Bắt đầu"),
            Map.entry("Stop", "Dừng"),
            Map.entry("Subnet Mask", "Mặt nạ mạng"),
            Map.entry("Subscriber", "Thuê bao"),
            Map.entry("Sunday", "Chủ Nhật"),
            Map.entry("TV", "TV"),
            Map.entry("Tap to Apply", "Chạm để áp dụng"),
            Map.entry("Temperature", "Nhiệt độ"),
            Map.entry("Text Message", "Tin nhắn văn bản"),
            Map.entry("Text Size", "Cỡ chữ"),
            Map.entry("Text Tone", "Âm báo tin nhắn"),
            Map.entry("The simulated phone follows the client system clock.", "Điện thoại mô phỏng sử dụng đồng hồ hệ thống của máy khách."),
            Map.entry("The simulated phone follows the selected region's time zone.", "Điện thoại mô phỏng sử dụng múi giờ của vùng đã chọn."),
            Map.entry("This updates the simulated VS:IA PhoneOS state; it does not replace your Minecraft mod files.", "Thao tác này cập nhật trạng thái PhoneOS mô phỏng của VS:IA; không thay thế các tệp mod Minecraft."),
            Map.entry("Thursday", "Thứ Năm"),
            Map.entry("Time", "Giờ"),
            Map.entry("Time Zone", "Múi giờ"),
            Map.entry("Time spent with the VS:IA phone UI open.", "Thời gian mở giao diện điện thoại VS:IA."),
            Map.entry("Tiếng Việt", "Tiếng Việt"),
            Map.entry("To:", "Đến:"),
            Map.entry("Today", "Hôm nay"),
            Map.entry("Touch", "Cảm ứng"),
            Map.entry("Transfer or Reset", "Chuyển hoặc đặt lại"),
            Map.entry("Transfer or Reset iPhone", "Chuyển hoặc đặt lại iPhone"),
            Map.entry("Tuesday", "Thứ Ba"),
            Map.entry("Unable to Check for Update", "Không thể kiểm tra bản cập nhật"),
            Map.entry("Unable to Join Network", "Không thể kết nối mạng"),
            Map.entry("Unknown", "Không rõ"),
            Map.entry("Update downloaded and ready to install.", "Bản cập nhật đã tải xong và sẵn sàng cài đặt."),
            Map.entry("VISION", "THỊ GIÁC"),
            Map.entry("VS:IA does not yet have a cloud backup service. Your server-side SIM and SMS data are not copied by this screen.", "VS:IA hiện chưa có dịch vụ sao lưu đám mây. Dữ liệu SIM và SMS phía máy chủ không được sao chép bởi màn hình này."),
            Map.entry("VoiceOver", "VoiceOver"),
            Map.entry("WORLD CLOCK", "GIỜ THẾ GIỚI"),
            Map.entry("Wallet", "Ví"),
            Map.entry("Wallet & Apple Pay", "Ví & Apple Pay"),
            Map.entry("Wallpaper", "Hình nền"),
            Map.entry("Weather", "Thời tiết"),
            Map.entry("Wednesday", "Thứ Tư"),
            Map.entry("What's new in Display & Text Size...", "Có gì mới trong Màn hình & Cỡ chữ..."),
            Map.entry("Wi-Fi", "Wi‑Fi"),
            Map.entry("Wi-Fi is off", "Wi‑Fi đang tắt"),
            Map.entry("Zoom", "Thu phóng"),
            Map.entry("iPhone Language", "Ngôn ngữ iPhone")
            );

    private PhoneI18n() {
    }

    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text == null ? "" : text;
        }

        String languageTag =
                PhoneLocaleSettings
                        .language()
                        .languageTag();

        String baseLanguage =
                languageTag == null
                        ? "en"
                        : languageTag
                        .split("-")[0]
                        .toLowerCase();

        if ("en".equals(baseLanguage)) {
            return text;
        }

        if ("vi".equals(baseLanguage)) {
            String exact = VIETNAMESE.get(text);

            if (exact != null) {
                return exact;
            }
        }

        String packed =
                externalTranslation(
                        languageTag,
                        text
                );

        if (packed != null) {
            return packed;
        }

        if (!baseLanguage.equals(languageTag)) {
            packed =
                    externalTranslation(
                            baseLanguage,
                            text
                    );

            if (packed != null) {
                return packed;
            }
        }

        if (text.startsWith("PhoneOS ") && text.endsWith(" is available.")) {
            String version =
                    text.substring(
                            "PhoneOS ".length(),
                            text.length() - " is available.".length()
                    );

            return "PhoneOS "
                    + version
                    + " đã sẵn sàng.";
        }

        if (text.startsWith("Downloading PhoneOS ")) {
            return "Đang tải "
                    + text.substring("Downloading ".length());
        }

        if (text.startsWith("From ")) {
            return "Từ "
                    + text.substring("From ".length());
        }

        if (text.startsWith("H:")) {
            return text;
        }

        return text;
    }

    public static void clearExternalPackCache() {
        EXTERNAL_PACK_CACHE.clear();
    }

    private static String externalTranslation(
            String languageTag,
            String sourceText
    ) {
        Map<String, String> pack =
                EXTERNAL_PACK_CACHE.computeIfAbsent(
                        languageTag,
                        PhoneI18n::loadExternalPack
                );

        return pack.get(sourceText);
    }

    private static Map<String, String> loadExternalPack(
            String languageTag
    ) {
        Map<String, String> result =
                new HashMap<>();

        try {
            Minecraft minecraft =
                    Minecraft.getInstance();

            Path path =
                    minecraft.gameDirectory
                            .toPath()
                            .resolve("config")
                            .resolve("vsia")
                            .resolve("phone_lang")
                            .resolve(
                                    languageTag
                                            .toLowerCase()
                                            + ".properties"
                            );

            if (!Files.exists(path)) {
                return Map.of();
            }

            Properties properties =
                    new Properties();

            try (Reader reader =
                         Files.newBufferedReader(
                                 path,
                                 StandardCharsets.UTF_8
                         )) {
                properties.load(reader);
            }

            for (String key : properties.stringPropertyNames()) {
                result.put(
                        key,
                        properties.getProperty(key)
                );
            }
        } catch (IOException ignored) {
        }

        return Map.copyOf(result);
    }

}

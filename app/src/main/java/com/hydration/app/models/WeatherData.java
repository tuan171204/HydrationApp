package com.hydration.app.models;

/**
 * MODEL: Lưu dữ liệu thời tiết sau khi parse từ JSON response của OpenWeather API
 *
 * JSON response mẫu:
 * {
 *   "main": { "temp": 33.5, "humidity": 80 },
 *   "weather": [{ "description": "scattered clouds" }],
 *   "name": "Hanoi"
 * }
 */
public class WeatherData {

    private double temperature;   // °C
    private int humidity;         // %
    private String description;   // "scattered clouds"
    private String cityName;      // "Hanoi"
    private boolean success;      // true nếu API call thành công
    private String errorMessage;  // thông báo lỗi nếu thất bại

    // Constructor thành công
    public WeatherData(double temperature, int humidity, String description, String cityName) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.description = description;
        this.cityName = cityName;
        this.success = true;
        this.errorMessage = "";
    }

    // Constructor thất bại
    public WeatherData(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
        this.temperature = 25.0; // fallback mặc định
    }

    public double getTemperature()  { return temperature; }
    public int getHumidity()        { return humidity; }
    public String getDescription()  { return description; }
    public String getCityName()     { return cityName; }
    public boolean isSuccess()      { return success; }
    public String getErrorMessage() { return errorMessage; }

    /**
     * Trả về icon thời tiết đơn giản dựa trên nhiệt độ
     * Dùng trong UI để hiển thị cảm giác thời tiết
     */
    public String getWeatherIcon() {
        if (temperature >= 35) return "🌡️";
        if (temperature >= 30) return "☀️";
        if (temperature >= 25) return "⛅";
        if (temperature >= 20) return "🌤️";
        return "❄️";
    }

    /**
     * Mô tả nhiệt độ bằng tiếng Việt để hiển thị cho user
     */
    public String getTemperatureLabel() {
        if (temperature >= 35) return "Nóng gay gắt";
        if (temperature >= 30) return "Nóng";
        if (temperature >= 25) return "Ấm áp";
        if (temperature >= 20) return "Mát mẻ";
        return "Lạnh";
    }
}

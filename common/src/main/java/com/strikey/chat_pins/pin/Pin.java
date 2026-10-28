package com.strikey.chat_pins.pin;

public record Pin(String id, String sender, String content, long timestamp, boolean system) {}
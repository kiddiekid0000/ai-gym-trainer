package com.aigymtrainer.backend.user.event;

import com.aigymtrainer.backend.user.domain.Status;

public record UserStatusChangedEvent(String email, Status oldStatus, Status newStatus) {
}
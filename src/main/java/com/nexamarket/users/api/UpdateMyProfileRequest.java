package com.nexamarket.users.api;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

public record UpdateMyProfileRequest(
        @Size(max = 100) String firstName,
        @Size(max = 100) String lastName,
        @Size(max = 30) String phoneNumber
) {
    @AssertTrue(message = "En az bir profil alanı güncellenmelidir")
    public boolean isChangeRequested() {
        return firstName != null || lastName != null || phoneNumber != null;
    }
}

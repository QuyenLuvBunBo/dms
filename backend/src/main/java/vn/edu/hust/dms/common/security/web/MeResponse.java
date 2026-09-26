package vn.edu.hust.dms.common.security.web;

import vn.edu.hust.dms.common.security.UserPrincipal;
import vn.edu.hust.dms.common.user.Role;

public record MeResponse(Long id, String username, String fullName, Role role) {

    public static MeResponse from(UserPrincipal principal) {
        return new MeResponse(principal.id(), principal.getUsername(), principal.fullName(), principal.role());
    }
}

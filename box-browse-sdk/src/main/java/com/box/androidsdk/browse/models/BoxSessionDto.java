package com.box.androidsdk.browse.models;

import android.content.Context;

import androidx.annotation.Nullable;

import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxSession;

import java.io.Serializable;

public class BoxSessionDto implements Serializable {

    public String userId;
    @Nullable
    public BoxAuthentication.AuthenticationRefreshProvider refreshProvider;

    public BoxSessionDto() {

    }

    public <E extends BoxAuthentication.AuthenticationRefreshProvider & Serializable>
    BoxSessionDto(final String userId, @Nullable final E refreshProvider) {
        this.userId = userId;
        this.refreshProvider = refreshProvider;
    }

    public static BoxSession unmarshal(final Context context, @Nullable final BoxSessionDto dto) {
        if (dto == null) {
            return null;
        }

        final BoxAuthentication.BoxAuthenticationInfo authInfo =
                BoxAuthentication.getInstance().getAuthInfo(dto.userId, context);
        return new BoxSession(context, authInfo,
                (Serializable & BoxAuthentication.AuthenticationRefreshProvider) dto.refreshProvider);
    }

    public static BoxSessionDto marshal(final BoxSession session) {
        final BoxSessionDto dto = new BoxSessionDto();
        dto.userId = session.getUserId();
        dto.refreshProvider = session.getRefreshProvider();
        return dto;
    }

}

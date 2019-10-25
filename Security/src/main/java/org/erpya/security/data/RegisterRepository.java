/*************************************************************************************
 * Product: Spin-Suite (Mobile Suite)                       		                 *
 * Copyright (C) 2012-2018 E.R.P. Consultores y Asociados, C.A.                      *
 * Contributor(s): Yamel Senih ysenih@erpya.com				  		                 *
 * This program is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU General Public License as published by              *
 * the Free Software Foundation, either version 3 of the License, or                 *
 * (at your option) any later version.                                               *
 * This program is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                    *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                     *
 * GNU General Public License for more details.                                      *
 * You should have received a copy of the GNU General Public License                 *
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.            *
 ************************************************************************************/
package org.erpya.security.data;

import org.erpya.base.util.Env;
import org.erpya.base.util.Util;
import org.erpya.security.data.model.RegisteredUser;
import org.erpya.security.util.SecureHandler;

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */
public class RegisterRepository {

    private static volatile RegisterRepository instance;

    private SecurityDataSource dataSource;

    // If user credentials will be cached in local storage, it is recommended it be encrypted
    // @see https://developer.android.com/training/articles/keystore
    private RegisteredUser user = null;

    // private constructor : singleton access
    private RegisterRepository(SecurityDataSource dataSource) {
        this.dataSource = dataSource;
        String userName = Env.getContext("#User_UserName");
        String email = Env.getContext("#User_UserEmail");
        String displayName = Env.getContext("#User_DisplayName");
        String token = Env.getContext("#SessionToken");
        if(!Util.isEmpty(token)) {
            user = new RegisteredUser(displayName, null,
                    SecureHandler.getInstance(Env.getContext()).getSecureEngine().decrypt(userName),
                    SecureHandler.getInstance(Env.getContext()).getSecureEngine().decrypt(email),
                    SecureHandler.getInstance(Env.getContext()).getSecureEngine().decrypt(token));
        }
    }

    public static RegisterRepository getInstance(SecurityDataSource dataSource) {
        if (instance == null) {
            instance = new RegisterRepository(dataSource);
        }
        return instance;
    }

    public boolean isLoggedIn() {
        return user != null;
    }

    public void logout() {
        if(user == null) {
            return;
        }
        dataSource.logout(user.getToken());
        user = null;
    }

    private void setRegisteredUser(RegisteredUser user) {
        this.user = user;
        String userName = SecureHandler.getInstance(Env.getContext()).getSecureEngine().encrypt(user.getUserName());
        String token = SecureHandler.getInstance(Env.getContext()).getSecureEngine().encrypt(user.getToken());
        String email = SecureHandler.getInstance(Env.getContext()).getSecureEngine().encrypt(user.getEmail());
        Env.setContext("#User_UserName", userName);
        Env.setContext("#User_UserEMail", email);
        Env.setContext("#SessionToken", token);
        Env.setContext("#User_DisplayName", user.getDisplayName());
    }

    public Result<RegisteredUser> enroll(String name, String username, String email) {
        // handle login
        Result<RegisteredUser> result = dataSource.enroll(name, username, email);
        if (result instanceof Result.Success) {
            setRegisteredUser(((Result.Success<RegisteredUser>) result).getData());
        }
        return result;
    }
}

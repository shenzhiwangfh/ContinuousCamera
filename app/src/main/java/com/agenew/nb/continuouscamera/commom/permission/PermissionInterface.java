package com.agenew.nb.continuouscamera.commom.permission;

public interface PermissionInterface {

    /**
     * 可设置请求权限请求码
     */
    int getPermissionsRequestCode();

    /**
     * 设置需要请求的权限
     */
    String[] getPermissions();

    /**
     * 请求权限回调
     */
    void permissionsResult(boolean success);
}
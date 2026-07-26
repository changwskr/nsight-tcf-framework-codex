package com.nh.nsight.common.session;

import java.io.Serializable;

public class LoginUserSession implements Serializable {
  private static final long serialVersionUID = 1L;
  private String userId;
  private String branchId;
  private String roleId;
  private String authLevel;
  private String maskingLevel;
  private String centerId;
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getBranchId() { return branchId; }
  public void setBranchId(String branchId) { this.branchId = branchId; }
  public String getRoleId() { return roleId; }
  public void setRoleId(String roleId) { this.roleId = roleId; }
  public String getAuthLevel() { return authLevel; }
  public void setAuthLevel(String authLevel) { this.authLevel = authLevel; }
  public String getMaskingLevel() { return maskingLevel; }
  public void setMaskingLevel(String maskingLevel) { this.maskingLevel = maskingLevel; }
  public String getCenterId() { return centerId; }
  public void setCenterId(String centerId) { this.centerId = centerId; }
}

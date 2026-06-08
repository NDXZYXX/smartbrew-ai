package com.smartbrew.smartbrew.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartbrew.smartbrew.entity.Device;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /** 根据心跳更新设备在线状态 */
    @Update("UPDATE device SET status = 1, ip_address = #{ip}, firmware_version = #{fwVer}, "
          + "last_heartbeat_time = NOW() WHERE device_id = #{deviceId}")
    int updateOnlineByHeartbeat(String deviceId, String ip, String fwVer);
}

package com.oy.oypicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceUserAnalyzeResponse implements Serializable {
    /**
     * 时间区间
     */
    private String period;

    /**
     * 某时间区间的上传数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}

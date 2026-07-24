package com.chinahitech.shop.service.impl;

import com.chinahitech.shop.bean.Inspection;
import com.chinahitech.shop.exception.EntityNotFoundException;
import com.chinahitech.shop.exception.UpdateException;
import com.chinahitech.shop.mapper.InspectionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("年审服务单元测试")
@ExtendWith(MockitoExtension.class)
class InspectionServiceImplTest {

    @Mock
    private InspectionMapper inspectionMapper;

    @InjectMocks
    private InspectionServiceImpl inspectionService;

    @Test
    @DisplayName("查询年审详情 - 记录不存在时抛出实体不存在异常")
    void testGetInspectionById_NotFound_ThrowException() {
        // Given - mapper cannot find inspection
        when(inspectionMapper.getById(404)).thenReturn(null);

        // When & Then - missing inspection is converted to service exception
        assertThrows(EntityNotFoundException.class, () -> inspectionService.getInspectionById(404));
    }

    @Test
    @DisplayName("查询附件 - 记录存在时返回附件地址")
    void testGetAttachment_InspectionExists_ReturnAttachment() {
        // Given - inspection has attachment
        Inspection inspection = inspection();
        inspection.setAttachment("uploads/inspection.pdf");
        when(inspectionMapper.getById(7)).thenReturn(inspection);

        // When - reading attachment
        String attachment = inspectionService.getAttachment(7);

        // Then - attachment path is returned
        assertEquals("uploads/inspection.pdf", attachment);
    }

    @Test
    @DisplayName("查询全部年审 - 空搜索条件走全量查询")
    void testFindAll_BlankSearch_QueryAll() {
        // Given - mapper has one inspection
        Inspection inspection = inspection();
        when(inspectionMapper.findAll()).thenReturn(Collections.singletonList(inspection));

        // When - search info is blank
        java.util.List<Inspection> result = inspectionService.findAll("  ");

        // Then - service uses full-list mapper method
        assertEquals(1, result.size());
        assertSame(inspection, result.get(0));
        verify(inspectionMapper).findAll();
    }

    @Test
    @DisplayName("查询全部年审 - 有搜索条件走模糊查询")
    void testFindAll_WithSearch_QueryBySearchInfo() {
        // Given - search result exists
        Inspection inspection = inspection();
        when(inspectionMapper.getBySearchInfo("摄影")).thenReturn(Collections.singletonList(inspection));

        // When - keyword search is provided
        java.util.List<Inspection> result = inspectionService.findAll("摄影");

        // Then - service uses search mapper method
        assertEquals(1, result.size());
        verify(inspectionMapper).getBySearchInfo("摄影");
    }

    @Test
    @DisplayName("新增年审 - 成功时初始化年份和时间字段")
    void testAddInspection_Success_InitializeMetadata() {
        // Given - mapper insert succeeds
        Inspection inspection = inspection();
        inspection.setId(99);
        inspection.setYear(Year.of(2020));
        when(inspectionMapper.insert(inspection)).thenReturn(1);

        // When - adding inspection
        inspectionService.addInspection(inspection);

        // Then - service rewrites managed fields before insert
        assertNull(inspection.getId());
        assertEquals(Year.now(), inspection.getYear());
        assertNotNull(inspection.getCreateTime());
        assertNotNull(inspection.getModifyTime());
        verify(inspectionMapper).insert(inspection);
    }

    @Test
    @DisplayName("新增年审 - 数据库写入失败时抛出修改异常")
    void testAddInspection_InsertFailed_ThrowException() {
        // Given - insert count indicates failure
        Inspection inspection = inspection();
        when(inspectionMapper.insert(inspection)).thenReturn(0);

        // When & Then - service reports failed write
        assertThrows(UpdateException.class, () -> inspectionService.addInspection(inspection));
    }

    @Test
    @DisplayName("填写反馈 - 成功时带上修改时间")
    void testAddFeedback_Success_UpdateFeedback() {
        // Given - update succeeds
        when(inspectionMapper.addFeedback(eq(7), eq("摄影社"), eq("材料完整"), any(Date.class))).thenReturn(1);
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

        // When - adding feedback
        inspectionService.addFeedback(7, "摄影社", "材料完整");

        // Then - mapper receives feedback and modify time
        verify(inspectionMapper).addFeedback(eq(7), eq("摄影社"), eq("材料完整"), dateCaptor.capture());
        assertNotNull(dateCaptor.getValue());
    }

    @Test
    @DisplayName("修改附件 - 更新0行时抛出修改异常")
    void testModifyAttachment_NoRowsUpdated_ThrowException() {
        // Given - mapper reports no row updated
        when(inspectionMapper.modifyAttachment(eq(7), eq("摄影社"), anyString(), any(Date.class))).thenReturn(0);

        // When & Then - missing target is reported
        assertThrows(UpdateException.class,
                () -> inspectionService.modifyAttachment(7, "摄影社", "uploads/new.pdf"));
    }

    @Test
    @DisplayName("审核年审 - 通过失败时抛出修改异常")
    void testConfirmApplication_UpdateFailed_ThrowException() {
        // Given - update count indicates failure
        when(inspectionMapper.confirmApplicationByid(7)).thenReturn(0);

        // When & Then - failed approval is reported
        assertThrows(UpdateException.class, () -> inspectionService.confirmApplication(7));
    }

    @Test
    @DisplayName("审核年审 - 拒绝成功时更新状态")
    void testDenyApplication_Success_UpdateStatus() {
        // Given - deny update succeeds
        when(inspectionMapper.denyApplicationByid(7)).thenReturn(1);

        // When - denying inspection
        inspectionService.denyApplication(7);

        // Then - mapper status method is called
        verify(inspectionMapper).denyApplicationByid(7);
    }

    private Inspection inspection() {
        Inspection inspection = new Inspection();
        inspection.setId(7);
        inspection.setGroupName("摄影社");
        return inspection;
    }
}

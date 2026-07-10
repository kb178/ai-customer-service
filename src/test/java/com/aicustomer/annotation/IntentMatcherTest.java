package com.aicustomer.annotation;

import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Course;
import com.aicustomer.service.CampusService;
import com.aicustomer.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * IntentMatcher 单元测试
 *
 * IntentMatcher 是 AI 对话系统的"耳朵"，负责从用户消息中识别：
 * 1. 意图（Intent）：用户想做什么？确认？取消？
 * 2. 实体（Entity）：用户提到的具体信息？课程？校区？学历？
 *
 * 意图通过 @Intent 注解定义（如 CONFIRM、CANCEL），关键词固定不变。
 * 实体分两类：
 * - 固定实体（interest、education）：通过 @EntityExtract 注解定义
 * - 动态实体（course、campus）：从数据库加载，新增数据自动生效
 *
 * 测试覆盖：
 * - 意图匹配的正确性和互斥性
 * - 各类实体提取的准确性
 * - 混合信息的一次性提取
 * - 数据库动态加载的课程/校区匹配
 */
@ExtendWith(MockitoExtension.class)
class IntentMatcherTest {

    @Mock
    private CampusService campusService;

    @Mock
    private CourseService courseService;

    @InjectMocks
    private IntentMatcher intentMatcher;

    /**
     * 初始化测试数据
     * Mock 数据库返回的课程和校区列表，模拟真实数据环境
     */
    @BeforeEach
    void setUp() {
        // 准备4门课程的Mock数据
        Course javaCourse = new Course();
        javaCourse.setId(1L);
        javaCourse.setName("Java全栈开发");
        javaCourse.setDescription("Java全栈开发,后端开发");

        Course pythonCourse = new Course();
        pythonCourse.setId(2L);
        pythonCourse.setName("Python数据分析");
        pythonCourse.setDescription("Python数据分析,数据分析");

        Course uiCourse = new Course();
        uiCourse.setId(3L);
        uiCourse.setName("UI/UX设计");
        uiCourse.setDescription("UI/UX设计,界面设计");

        Course aiCourse = new Course();
        aiCourse.setId(4L);
        aiCourse.setName("人工智能入门");
        aiCourse.setDescription("人工智能入门,AI");

        // 当查询课程时，返回上面4门课程
        when(courseService.searchCourses(null, null)).thenReturn(
                Arrays.asList(javaCourse, pythonCourse, uiCourse, aiCourse));

        // 准备3个校区的Mock数据
        Campus zhongguancun = new Campus();
        zhongguancun.setId(1L);
        zhongguancun.setName("中关村校区");

        Campus guomao = new Campus();
        guomao.setId(2L);
        guomao.setName("国贸校区");

        Campus xizhimen = new Campus();
        xizhimen.setId(3L);
        xizhimen.setName("西直门校区");

        // 当查询校区时，返回上面3个校区
        when(campusService.getAllCampuses()).thenReturn(
                Arrays.asList(zhongguancun, guomao, xizhimen));

        // 重新扫描注解并加载数据库数据
        // 必须在 Mock 设置之后调用，因为 init() 会调用数据库查询
        intentMatcher.init();
    }

    // ==================== matchIntent 意图匹配测试 ====================

    /**
     * 测试：各种确认词都能匹配 CONFIRM 意图
     * 用户说这些词时，系统应执行待确认的预约修改/取消操作
     */
    @Test
    void matchIntent_确认意图_包含确认词() {
        assertTrue(intentMatcher.matchIntent("确认", "CONFIRM"));
        assertTrue(intentMatcher.matchIntent("确定", "CONFIRM"));
        assertTrue(intentMatcher.matchIntent("好的", "CONFIRM"));
        assertTrue(intentMatcher.matchIntent("没问题", "CONFIRM"));
        assertTrue(intentMatcher.matchIntent("OK", "CONFIRM"));
    }

    /**
     * 测试：确认词不应匹配 CANCEL 意图
     * 防止"确认"被错误识别为"取消"
     */
    @Test
    void matchIntent_确认意图_不匹配取消词() {
        assertFalse(intentMatcher.matchIntent("取消", "CONFIRM"));
        assertFalse(intentMatcher.matchIntent("不要了", "CONFIRM"));
    }

    /**
     * 测试：各种取消词都能匹配 CANCEL 意图
     * 用户说这些词时，系统应执行预约取消流程
     */
    @Test
    void matchIntent_取消意图() {
        assertTrue(intentMatcher.matchIntent("取消", "CANCEL"));
        assertTrue(intentMatcher.matchIntent("不要了", "CANCEL"));
        assertTrue(intentMatcher.matchIntent("算了", "CANCEL"));
        assertTrue(intentMatcher.matchIntent("不去了", "CANCEL"));
    }

    /**
     * 测试：取消词不应匹配 CONFIRM 意图
     * 防止"取消"被错误识别为"确认"
     */
    @Test
    void matchIntent_取消意图_不匹配确认词() {
        assertFalse(intentMatcher.matchIntent("确认", "CANCEL"));
        assertFalse(intentMatcher.matchIntent("好的", "CANCEL"));
    }

    /**
     * 测试：不存在的意图名称应返回 false
     * 防御性测试：确保不会因为错误的意图名导致异常
     */
    @Test
    void matchIntent_不存在的意图_返回false() {
        assertFalse(intentMatcher.matchIntent("确认", "UNKNOWN"));
    }

    /**
     * 测试：意图匹配应大小写不敏感
     * 用户输入"OK"或"ok"都应被识别为确认
     */
    @Test
    void matchIntent_大小写不敏感() {
        assertTrue(intentMatcher.matchIntent("Ok", "CONFIRM"));
        assertTrue(intentMatcher.matchIntent("OK", "CONFIRM"));
    }

    // ==================== extractEntities 实体提取测试 ====================

    // ---------- 兴趣提取 ----------

    /**
     * 测试：从"我想学编程"中提取兴趣为"编程开发"
     * "编程"是 @EntityExtract 注解中定义的关键词之一
     */
    @Test
    void extractEntities_兴趣_编程开发() {
        Map<String, String> result = intentMatcher.extractEntities("我想学编程");
        assertEquals("编程开发", result.get("interest"));
    }

    /**
     * 测试：从"我对UI设计感兴趣"中提取兴趣为"UI/UX设计"
     * "UI"是设计类兴趣的关键词
     */
    @Test
    void extractEntities_兴趣_UI设计() {
        Map<String, String> result = intentMatcher.extractEntities("我对UI设计感兴趣");
        assertEquals("UI/UX设计", result.get("interest"));
    }

    /**
     * 测试：从"我想做数据分析"中提取兴趣为"数据分析"
     * "数据"是数据分析类兴趣的关键词
     */
    @Test
    void extractEntities_兴趣_数据分析() {
        Map<String, String> result = intentMatcher.extractEntities("我想做数据分析");
        assertEquals("数据分析", result.get("interest"));
    }

    // ---------- 学历提取 ----------

    /**
     * 测试：从"我是研究生"中提取学历
     * 学历信息帮助 AI 推荐合适难度的课程
     */
    @Test
    void extractEntities_学历_研究生() {
        Map<String, String> result = intentMatcher.extractEntities("我是研究生");
        assertEquals("研究生", result.get("education"));
    }

    /**
     * 测试：从"我是大一新生"中提取学历为"大一"
     */
    @Test
    void extractEntities_学历_大一() {
        Map<String, String> result = intentMatcher.extractEntities("我是大一新生");
        assertEquals("大一", result.get("education"));
    }

    /**
     * 测试：从"我零基础"中提取学历为"零基础"
     * "零基础"是比较特殊的学历描述，需要特别处理
     */
    @Test
    void extractEntities_学历_零基础() {
        Map<String, String> result = intentMatcher.extractEntities("我零基础");
        assertEquals("零基础", result.get("education"));
    }

    // ---------- 课程提取（从数据库动态匹配） ----------

    /**
     * 测试：从"我想学Java"中匹配到 Java全栈开发 课程
     * 课程关键词从数据库加载，"java"能匹配到课程名中的"Java"
     */
    @Test
    void extractEntities_课程_Java() {
        Map<String, String> result = intentMatcher.extractEntities("我想学Java");
        assertEquals("Java全栈开发", result.get("course"));
    }

    /**
     * 测试：从"python怎么样"中匹配到 Python数据分析 课程
     */
    @Test
    void extractEntities_课程_Python() {
        Map<String, String> result = intentMatcher.extractEntities("python怎么样");
        assertEquals("Python数据分析", result.get("course"));
    }

    /**
     * 测试：从"学设计"中匹配到 UI/UX设计 课程
     * "设计"是课程描述中的关键词
     */
    @Test
    void extractEntities_课程_UI设计() {
        Map<String, String> result = intentMatcher.extractEntities("学设计");
        assertEquals("UI/UX设计", result.get("course"));
    }

    /**
     * 测试：从"人工智能好学吗"中匹配到 人工智能入门 课程
     */
    @Test
    void extractEntities_课程_人工智能() {
        Map<String, String> result = intentMatcher.extractEntities("人工智能好学吗");
        assertEquals("人工智能入门", result.get("course"));
    }

    // ---------- 校区提取（从数据库动态匹配） ----------

    /**
     * 测试：从"中关村有校区吗"中匹配到 中关村校区
     * 校区名去掉"校区"后缀也能匹配
     */
    @Test
    void extractEntities_校区_中关村() {
        Map<String, String> result = intentMatcher.extractEntities("中关村有校区吗");
        assertEquals("中关村校区", result.get("campus"));
    }

    /**
     * 测试：从"国贸校区在哪"中匹配到 国贸校区
     */
    @Test
    void extractEntities_校区_国贸() {
        Map<String, String> result = intentMatcher.extractEntities("国贸校区在哪");
        assertEquals("国贸校区", result.get("campus"));
    }

    /**
     * 测试：从"西直门校区"中匹配到 西直门校区
     */
    @Test
    void extractEntities_校区_西直门() {
        Map<String, String> result = intentMatcher.extractEntities("西直门校区");
        assertEquals("西直门校区", result.get("campus"));
    }

    // ---------- 混合信息提取 ----------

    /**
     * 测试：一句话中包含学历+课程+校区，能全部提取
     * 验证多个实体不会互相干扰
     */
    @Test
    void extractEntities_混合信息_全部提取() {
        Map<String, String> result = intentMatcher.extractEntities(
                "我是大三学生，想学python，国贸校区有吗");
        assertEquals("大三", result.get("education"));
        assertEquals("Python数据分析", result.get("course"));
        assertEquals("国贸校区", result.get("campus"));
    }

    /**
     * 测试：无关消息不提取任何实体
     * 用户说"今天天气真好"时不应误提取
     */
    @Test
    void extractEntities_无匹配信息_返回空map() {
        Map<String, String> result = intentMatcher.extractEntities("今天天气真好");
        assertTrue(result.isEmpty());
    }

    // ==================== lookupCourse 课程查找测试 ====================

    /**
     * 测试：能通过课程名找到对应课程
     * 课程名小写也能匹配
     */
    @Test
    void lookupCourse_存在的课程_返回课程() {
        Course course = intentMatcher.lookupCourse("java全栈开发");
        assertNotNull(course);
        assertEquals(1L, course.getId());
    }

    /**
     * 测试：查找不存在的课程返回 null
     */
    @Test
    void lookupCourse_不存在的课程_返回null() {
        assertNull(intentMatcher.lookupCourse("不存在的课程"));
    }

    /**
     * 测试：传入 null 不会报错，返回 null
     * 防御性测试
     */
    @Test
    void lookupCourse_null_返回null() {
        assertNull(intentMatcher.lookupCourse(null));
    }

    // ==================== lookupCampus 校区查找测试 ====================

    /**
     * 测试：能通过完整校区名找到对应校区
     */
    @Test
    void lookupCampus_存在的校区_返回校区() {
        Campus campus = intentMatcher.lookupCampus("中关村校区");
        assertNotNull(campus);
        assertEquals(1L, campus.getId());
    }

    /**
     * 测试：去掉"校区"后缀的短名称也能匹配
     * 用户说"中关村"而不是"中关村校区"时也能识别
     */
    @Test
    void lookupCampus_短名称也能匹配() {
        Campus campus = intentMatcher.lookupCampus("中关村");
        assertNotNull(campus);
        assertEquals(1L, campus.getId());
    }

    /**
     * 测试：查找不存在的校区返回 null
     */
    @Test
    void lookupCampus_不存在的校区_返回null() {
        assertNull(intentMatcher.lookupCampus("不存在的校区"));
    }
}

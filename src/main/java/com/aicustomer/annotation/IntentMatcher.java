package com.aicustomer.annotation;

import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Course;
import com.aicustomer.service.CampusService;
import com.aicustomer.service.CourseService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 意图匹配器
 *
 * - 意图识别（CONFIRM/CANCEL）：通过注解定义，固定不变
 * - 实体识别（interest/education）：通过注解定义，分类固定
 * - 实体识别（course/campus）：从数据库动态加载，新增数据自动生效
 */
@Component
@RequiredArgsConstructor
public class IntentMatcher {

    private final CampusService campusService;
    private final CourseService courseService;

    /** intentName → keywords */
    private final Map<String, List<String>> intentKeywords = new LinkedHashMap<>();

    /** field → [{ keywords, value }]（仅注解驱动的：interest、education） */
    private final Map<String, List<EntityMapping>> entityMappings = new LinkedHashMap<>();

    /** 动态加载的校区关键词：keyword(如"中关村") → Campus实体 */
    private final Map<String, Campus> campusKeywordMap = new LinkedHashMap<>();

    /** 动态加载的课程关键词：keyword(如"java") → Course实体 */
    private final Map<String, Course> courseKeywordMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        scanAnnotations();
        loadFromDatabase();
    }

    /**
     * 扫描注解定义（意图 + interest/education）
     */
    private void scanAnnotations() {
        for (Method method : getClass().getMethods()) {
            Intent intent = method.getAnnotation(Intent.class);
            if (intent != null) {
                intentKeywords.put(intent.name(), Arrays.asList(intent.keywords()));
            }

            EntityExtract entity = method.getAnnotation(EntityExtract.class);
            if (entity != null) {
                String field = entity.field();
                // 跳过 course 和 campus，这两个从数据库加载
                if ("course".equals(field) || "campus".equals(field)) continue;

                List<EntityMapping> mappings = new ArrayList<>();
                for (KeywordMapping km : entity.mappings()) {
                    mappings.add(new EntityMapping(km.keywords(), km.value()));
                }
                entityMappings.put(field, mappings);
            }
        }
    }

    /**
     * 从数据库动态加载课程和校区关键词
     */
    private void loadFromDatabase() {
        // 加载课程：用课程名 + 别名作为关键词
        List<Course> courses = courseService.searchCourses(null, null);
        for (Course course : courses) {
            // 课程名本身作为关键词（如 "Java全栈开发"）
            courseKeywordMap.put(course.getName().toLowerCase(), course);
            // 课程名去掉空格/特殊字符也作为关键词
            String simplified = course.getName().toLowerCase()
                    .replaceAll("[/\\s]", "")
                    .replace("开发", "")
                    .replace("设计", "")
                    .replace("入门", "");
            if (!simplified.isEmpty() && !courseKeywordMap.containsKey(simplified)) {
                courseKeywordMap.put(simplified, course);
            }
            // 描述中的关键词也加入
            if (course.getDescription() != null) {
                String[] descWords = course.getDescription().toLowerCase().split("[,，、]");
                for (String word : descWords) {
                    String w = word.trim();
                    if (!w.isEmpty() && !courseKeywordMap.containsKey(w)) {
                        courseKeywordMap.put(w, course);
                    }
                }
            }
        }

        // 加载校区：用校区名作为关键词
        List<Campus> campuses = campusService.getAllCampuses();
        for (Campus campus : campuses) {
            campusKeywordMap.put(campus.getName().toLowerCase(), campus);
            // 去掉"校区"后缀也作为关键词（如用户说"中关村"而不是"中关村校区"）
            String shortName = campus.getName().toLowerCase().replace("校区", "");
            if (!shortName.isEmpty() && !campusKeywordMap.containsKey(shortName)) {
                campusKeywordMap.put(shortName, campus);
            }
        }
    }

    /**
     * 检查消息是否匹配指定意图
     */
    public boolean matchIntent(String message, String intentName) {
        String lowerMsg = message.toLowerCase();
        List<String> keywords = intentKeywords.get(intentName);
        if (keywords == null) return false;
        return keywords.stream().anyMatch(lowerMsg::contains);
    }

    /**
     * 从消息中提取实体信息
     * @return field → value 的映射（course/campus 返回的是名称，ID通过 lookup 方法获取）
     */
    public Map<String, String> extractEntities(String message) {
        String lowerMsg = message.toLowerCase();
        Map<String, String> result = new LinkedHashMap<>();

        // 注解驱动的实体提取（interest、education）
        for (Map.Entry<String, List<EntityMapping>> entry : entityMappings.entrySet()) {
            String field = entry.getKey();
            for (EntityMapping mapping : entry.getValue()) {
                for (String keyword : mapping.keywords()) {
                    if (lowerMsg.contains(keyword.toLowerCase())) {
                        result.put(field, mapping.value());
                        break;
                    }
                }
                //如果学历或是兴趣已经匹配到了就跳出，不用还接着匹配
                if (result.containsKey(field)) break;
            }
        }

        // 动态加载的课程匹配
        if (!result.containsKey("course")) {
            Course matched = matchCourse(lowerMsg);
            if (matched != null) {
                result.put("course", matched.getName());
            }
        }

        // 动态加载的校区匹配
        if (!result.containsKey("campus")) {
            Campus matched = matchCampus(lowerMsg);
            if (matched != null) {
                result.put("campus", matched.getName());
            }
        }

        return result;
    }

    /**
     * 根据课程名查找课程ID
     */
    public Course lookupCourse(String courseName) {
        if (courseName == null) return null;
        return courseKeywordMap.get(courseName.toLowerCase());
    }

    /**
     * 根据校区名查找校区ID
     */
    public Campus lookupCampus(String campusName) {
        if (campusName == null) return null;
        return campusKeywordMap.get(campusName.toLowerCase());
    }

    private Course matchCourse(String lowerMsg) {
        // 优先精确匹配（长关键词优先）
        Course bestMatch = null;
        int bestLength = 0;
        for (Map.Entry<String, Course> entry : courseKeywordMap.entrySet()) {
            if (lowerMsg.contains(entry.getKey()) && entry.getKey().length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = entry.getKey().length();
            }
        }
        return bestMatch;
    }

    private Campus matchCampus(String lowerMsg) {
        Campus bestMatch = null;
        int bestLength = 0;
        for (Map.Entry<String, Campus> entry : campusKeywordMap.entrySet()) {
            if (lowerMsg.contains(entry.getKey()) && entry.getKey().length() > bestLength) {
                bestMatch = entry.getValue();
                bestLength = entry.getKey().length();
            }
        }
        return bestMatch;
    }

    // ========== Intent 定义（固定不变的意图） ==========

    @Intent(name = "CONFIRM", keywords = {"确认", "确定", "没问题", "好的", "可以", "ok", "对的", "是的"})
    public void confirmIntent() {}

    @Intent(name = "CANCEL", keywords = {"取消", "不要了", "算了", "不去了"})
    public void cancelIntent() {}

    // ========== EntityExtract 定义（固定分类） ==========

    @EntityExtract(field = "interest", mappings = {
        @KeywordMapping(keywords = {"编程", "java", "python", "前端", "后端", "开发"}, value = "编程开发"),
        @KeywordMapping(keywords = {"设计", "ui", "ux"}, value = "UI/UX设计"),
        @KeywordMapping(keywords = {"数据", "ai", "人工智能", "分析"}, value = "数据分析")
    })
    public void extractInterest() {}

    @EntityExtract(field = "education", mappings = {
        @KeywordMapping(keywords = {"大一", "大一新生"}, value = "大一"),
        @KeywordMapping(keywords = {"大二"}, value = "大二"),
        @KeywordMapping(keywords = {"大三"}, value = "大三"),
        @KeywordMapping(keywords = {"大四"}, value = "大四"),
        @KeywordMapping(keywords = {"研究生", "硕士"}, value = "研究生"),
        @KeywordMapping(keywords = {"博士"}, value = "博士"),
        @KeywordMapping(keywords = {"高中", "高三"}, value = "高中"),
        @KeywordMapping(keywords = {"零基础", "没有基础"}, value = "零基础")
    })
    public void extractEducation() {}

    // ========== 内部数据类 ==========

    private static class EntityMapping {
        private final String[] keywords;
        private final String value;

        EntityMapping(String[] keywords, String value) {
            this.keywords = keywords;
            this.value = value;
        }

        public String[] keywords() { return keywords; }
        public String value() { return value; }
    }
}

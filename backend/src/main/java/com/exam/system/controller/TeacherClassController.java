package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.ClassInfo;
import com.exam.system.entity.Student;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.mapper.StudentMapper;
import com.exam.system.service.AuditLogService;
import com.exam.system.util.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/teacher")
public class TeacherClassController {
    private static final Logger log = LoggerFactory.getLogger(TeacherClassController.class);

    private final ClassInfoMapper classInfoMapper;
    private final StudentMapper studentMapper;
    private final AuditLogService auditLogService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TeacherClassController(ClassInfoMapper classInfoMapper,
                                  StudentMapper studentMapper,
                                  AuditLogService auditLogService) {
        this.classInfoMapper = classInfoMapper;
        this.studentMapper = studentMapper;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/classes")
    public List<ClassInfo> getClasses() {
        LambdaQueryWrapper<ClassInfo> query = new LambdaQueryWrapper<>();
        query.orderByDesc(ClassInfo::getId);
        return classInfoMapper.selectList(query);
    }

    @PostMapping("/classes")
    public void createClass(@RequestBody ClassInfo classInfo) {
        if (classInfo.getName() == null || classInfo.getName().isBlank()) {
            throw new IllegalArgumentException("班级名称不能为空");
        }
        long exists = classInfoMapper.selectCount(new LambdaQueryWrapper<ClassInfo>().eq(ClassInfo::getName, classInfo.getName().trim()));
        if (exists > 0) {
            throw new IllegalArgumentException("班级名称已存在");
        }
        classInfo.setName(classInfo.getName().trim());
        classInfo.setCreateTime(LocalDateTime.now());
        classInfo.setStudentCount(0);
        classInfoMapper.insert(classInfo);
    }

    @PutMapping("/classes/{id}")
    public void renameClass(@PathVariable Long id, @RequestBody ClassInfo payload) {
        ClassInfo classInfo = classInfoMapper.selectById(id);
        if (classInfo == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        String newName = payload.getName() == null ? "" : payload.getName().trim();
        if (newName.isBlank()) {
            throw new IllegalArgumentException("班级名称不能为空");
        }
        long duplicated = classInfoMapper.selectCount(
                new LambdaQueryWrapper<ClassInfo>()
                        .eq(ClassInfo::getName, newName)
                        .ne(ClassInfo::getId, id)
        );
        if (duplicated > 0) {
            throw new IllegalArgumentException("班级名称已存在");
        }
        classInfo.setName(newName);
        classInfoMapper.updateById(classInfo);
    }

    @DeleteMapping("/classes/{id}")
    public void deleteClass(@PathVariable Long id) {
        classInfoMapper.deleteById(id);
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getClassId, id);
        studentMapper.delete(wrapper);
    }

    @GetMapping("/classes/{classId}/students")
    public List<Student> getStudentsByClass(@PathVariable Long classId) {
        LambdaQueryWrapper<Student> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Student::getClassId, classId);
        return studentMapper.selectList(wrapper);
    }

    @GetMapping("/classes/{classId}/students/page")
    public Map<String, Object> getStudentsByClassPage(@PathVariable Long classId,
                                                      @RequestParam(defaultValue = "1") Integer page,
                                                      @RequestParam(defaultValue = "20") Integer size,
                                                      @RequestParam(required = false) String keyword) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        int safeSize = (size == null || size <= 0) ? 20 : Math.min(size, 200);
        int safePage = (page == null || page <= 0) ? 1 : page;
        int offset = (safePage - 1) * safeSize;

        LambdaQueryWrapper<Student> countWrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId);
        if (!kw.isEmpty()) {
            countWrapper.and(w -> w.like(Student::getName, kw).or().like(Student::getStudentNo, kw));
        }
        int total = Math.toIntExact(studentMapper.selectCount(countWrapper));

        LambdaQueryWrapper<Student> listWrapper = new LambdaQueryWrapper<Student>()
                .eq(Student::getClassId, classId)
                .orderByDesc(Student::getId)
                .last("limit " + offset + "," + safeSize);
        if (!kw.isEmpty()) {
            listWrapper.and(w -> w.like(Student::getName, kw).or().like(Student::getStudentNo, kw));
        }
        List<Student> list = studentMapper.selectList(listWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    @GetMapping("/classes/{classId}/students/export")
    public ResponseEntity<byte[]> exportStudentsCsv(@PathVariable Long classId) {
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new IllegalArgumentException("班级不存在");
        }
        List<Student> students = getStudentsByClass(classId);

        StringBuilder sb = new StringBuilder();
        sb.append("学号,姓名,联系电话,加入时间\n");
        for (Student s : students) {
            sb.append(csvSafe(s.getStudentNo())).append(",")
                    .append(csvSafe(s.getName())).append(",")
                    .append(csvSafe(s.getPhone())).append(",")
                    .append(csvSafe(s.getCreateTime() == null ? "" : s.getCreateTime().toString()))
                    .append("\n");
        }
        byte[] content = sb.toString().getBytes(StandardCharsets.UTF_8);
        String fileName = classInfo.getName().replaceAll("\\s+", "_") + "_students.csv";

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .body(content);
    }

    @PostMapping("/students")
    public void addStudent(@RequestBody Student student) {
        if (student.getClassId() == null) {
            throw new IllegalArgumentException("班级不能为空");
        }
        if (student.getStudentNo() == null || student.getStudentNo().isBlank()) {
            throw new IllegalArgumentException("学号不能为空");
        }
        if (student.getName() == null || student.getName().isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, student.getStudentNo().trim()));
        if (exists > 0) {
            throw new IllegalArgumentException("学号已存在");
        }
        student.setStudentNo(student.getStudentNo().trim());
        student.setName(student.getName().trim());
        student.setCreateTime(LocalDateTime.now());
        // Store default password in bcrypt format; initial raw password remains 123456.
        student.setPassword(passwordEncoder.encode("123456"));
        studentMapper.insert(student);

        ClassInfo classInfo = classInfoMapper.selectById(student.getClassId());
        if (classInfo != null) {
            classInfo.setStudentCount(classInfo.getStudentCount() + 1);
            classInfoMapper.updateById(classInfo);
        }
    }

    @PostMapping("/students/batch")
    public Map<String, Object> batchAddStudents(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        Object classIdRaw = payload.get("classId");
        Object studentsRaw = payload.get("students");
        if (classIdRaw == null || !(studentsRaw instanceof List<?> students)) {
            throw new IllegalArgumentException("参数错误");
        }

        Long classId = Long.valueOf(String.valueOf(classIdRaw));
        ClassInfo classInfo = classInfoMapper.selectById(classId);
        if (classInfo == null) {
            throw new IllegalArgumentException("班级不存在");
        }

        int success = 0;
        int skipped = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            Object item = students.get(i);
            if (!(item instanceof Map<?, ?> rawRow)) {
                skipped++;
                errors.add(buildBatchError(i + 1, "", "", "行数据格式错误"));
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rawRow;
            String studentNo = String.valueOf(row.getOrDefault("studentNo", "")).trim();
            String name = String.valueOf(row.getOrDefault("name", "")).trim();
            String phone = String.valueOf(row.getOrDefault("phone", "")).trim();
            if (studentNo.isBlank() || name.isBlank()) {
                skipped++;
                errors.add(buildBatchError(i + 1, studentNo, name, "学号或姓名为空"));
                continue;
            }

            long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo));
            if (exists > 0) {
                skipped++;
                errors.add(buildBatchError(i + 1, studentNo, name, "学号已存在"));
                continue;
            }

            Student student = new Student();
            student.setClassId(classId);
            student.setStudentNo(studentNo);
            student.setName(name);
            student.setPhone(phone.isBlank() ? null : phone);
            student.setCreateTime(LocalDateTime.now());
            student.setPassword(passwordEncoder.encode("123456"));
            studentMapper.insert(student);
            success++;
        }

        if (success > 0) {
            classInfo.setStudentCount(classInfo.getStudentCount() + success);
            classInfoMapper.updateById(classInfo);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        result.put("errors", errors);
        log.info("AUDIT batch_import_students operator={} classId={} success={} skipped={}",
                CurrentUser.loginName(request), classId, success, skipped);
        auditLogService.record(
                CurrentUser.userId(request),
                "BATCH_IMPORT_STUDENTS",
                "CLASS",
                classId,
                "success=" + success + ",skipped=" + skipped
        );
        return result;
    }

    @DeleteMapping("/students/{id}")
    public void removeStudent(@PathVariable Long id) {
        Student student = studentMapper.selectById(id);
        if (student != null) {
            studentMapper.deleteById(id);
            ClassInfo classInfo = classInfoMapper.selectById(student.getClassId());
            if (classInfo != null && classInfo.getStudentCount() > 0) {
                classInfo.setStudentCount(classInfo.getStudentCount() - 1);
                classInfoMapper.updateById(classInfo);
            }
        }
    }

    @PutMapping("/students/{id}")
    public void updateStudent(@PathVariable Long id, @RequestBody Student payload, HttpServletRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        String name = payload.getName() == null ? "" : payload.getName().trim();
        String phone = payload.getPhone() == null ? "" : payload.getPhone().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("学生姓名不能为空");
        }
        student.setName(name);
        student.setPhone(phone.isBlank() ? null : phone);
        studentMapper.updateById(student);
        log.info("AUDIT update_student operator={} studentId={} studentNo={}",
                CurrentUser.loginName(request), id, student.getStudentNo());
        auditLogService.record(
                CurrentUser.userId(request),
                "UPDATE_STUDENT",
                "STUDENT",
                id,
                "studentNo=" + student.getStudentNo()
        );
    }

    @PostMapping("/students/{id}/reset-password")
    public String resetStudentPassword(@PathVariable Long id, HttpServletRequest request) {
        Student student = studentMapper.selectById(id);
        if (student == null) {
            throw new IllegalArgumentException("学生不存在");
        }
        student.setPassword(passwordEncoder.encode("123456"));
        studentMapper.updateById(student);
        log.info("AUDIT reset_student_password operator={} studentId={} studentNo={}",
                CurrentUser.loginName(request), id, student.getStudentNo());
        auditLogService.record(
                CurrentUser.userId(request),
                "RESET_STUDENT_PASSWORD",
                "STUDENT",
                id,
                "studentNo=" + student.getStudentNo()
        );
        return "已重置为初始密码 123456";
    }

    @PostMapping("/students/batch-reset-password")
    public String batchResetPassword(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        List<Long> studentIds = parseStudentIds(payload.get("studentIds"));
        if (studentIds.isEmpty()) {
            throw new IllegalArgumentException("studentIds 不能为空");
        }
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        for (Student student : students) {
            student.setPassword(passwordEncoder.encode("123456"));
            studentMapper.updateById(student);
        }
        auditLogService.record(
                CurrentUser.userId(request),
                "BATCH_RESET_STUDENT_PASSWORD",
                "STUDENT",
                null,
                "count=" + students.size() + ",studentIds=" + studentIds
        );
        return "已批量重置 " + students.size() + " 名学生密码";
    }

    @PostMapping("/students/batch-remove")
    public String batchRemoveStudents(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        List<Long> studentIds = parseStudentIds(payload.get("studentIds"));
        if (studentIds.isEmpty()) {
            throw new IllegalArgumentException("studentIds 不能为空");
        }
        List<Student> students = studentMapper.selectBatchIds(studentIds);
        if (students.isEmpty()) {
            return "未匹配到可删除学生";
        }
        Map<Long, Long> removeByClass = students.stream()
                .filter(x -> x.getClassId() != null)
                .collect(Collectors.groupingBy(Student::getClassId, Collectors.counting()));
        for (Student student : students) {
            studentMapper.deleteById(student.getId());
        }
        for (Map.Entry<Long, Long> entry : removeByClass.entrySet()) {
            ClassInfo classInfo = classInfoMapper.selectById(entry.getKey());
            if (classInfo == null) continue;
            int oldCount = classInfo.getStudentCount() == null ? 0 : classInfo.getStudentCount();
            int newCount = Math.max(0, oldCount - entry.getValue().intValue());
            classInfo.setStudentCount(newCount);
            classInfoMapper.updateById(classInfo);
        }
        auditLogService.record(
                CurrentUser.userId(request),
                "BATCH_REMOVE_STUDENT",
                "STUDENT",
                null,
                "count=" + students.size() + ",studentIds=" + studentIds
        );
        return "已移出 " + students.size() + " 名学生";
    }

    @PostMapping("/students/batch-move")
    public String batchMoveStudents(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        List<Long> studentIds = parseStudentIds(payload.get("studentIds"));
        Object targetClassIdRaw = payload.get("targetClassId");
        if (studentIds.isEmpty() || targetClassIdRaw == null) {
            throw new IllegalArgumentException("studentIds/targetClassId 不能为空");
        }
        Long targetClassId = Long.valueOf(String.valueOf(targetClassIdRaw));
        ClassInfo targetClass = classInfoMapper.selectById(targetClassId);
        if (targetClass == null) {
            throw new IllegalArgumentException("目标班级不存在");
        }

        List<Student> students = studentMapper.selectBatchIds(studentIds);
        if (students.isEmpty()) {
            return "未匹配到可移动学生";
        }
        Map<Long, Long> sourceClassCount = new HashMap<>();
        int moved = 0;
        for (Student student : students) {
            if (targetClassId.equals(student.getClassId())) {
                continue;
            }
            if (student.getClassId() != null) {
                sourceClassCount.merge(student.getClassId(), 1L, Long::sum);
            }
            student.setClassId(targetClassId);
            studentMapper.updateById(student);
            moved++;
        }

        for (Map.Entry<Long, Long> entry : sourceClassCount.entrySet()) {
            ClassInfo source = classInfoMapper.selectById(entry.getKey());
            if (source == null) continue;
            int oldCount = source.getStudentCount() == null ? 0 : source.getStudentCount();
            source.setStudentCount(Math.max(0, oldCount - entry.getValue().intValue()));
            classInfoMapper.updateById(source);
        }
        if (moved > 0) {
            int targetCount = targetClass.getStudentCount() == null ? 0 : targetClass.getStudentCount();
            targetClass.setStudentCount(targetCount + moved);
            classInfoMapper.updateById(targetClass);
        }

        auditLogService.record(
                CurrentUser.userId(request),
                "BATCH_MOVE_STUDENT",
                "CLASS",
                targetClassId,
                "moved=" + moved + ",studentIds=" + studentIds
        );
        return "已移动 " + moved + " 名学生到目标班级";
    }

    private String csvSafe(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private Map<String, Object> buildBatchError(int lineNo, String studentNo, String name, String reason) {
        Map<String, Object> row = new HashMap<>();
        row.put("lineNo", lineNo);
        row.put("studentNo", studentNo == null ? "" : studentNo);
        row.put("name", name == null ? "" : name);
        row.put("reason", reason);
        return row;
    }

    private List<Long> parseStudentIds(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return Collections.emptyList();
        }
        Set<Long> ids = list.stream()
                .map(String::valueOf)
                .map(String::trim)
                .filter(x -> !x.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
        return new ArrayList<>(ids);
    }
}

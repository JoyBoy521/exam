package com.exam.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.system.entity.ClassInfo;
import com.exam.system.entity.Student;
import com.exam.system.mapper.ClassInfoMapper;
import com.exam.system.mapper.StudentMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherClassController {

    private final ClassInfoMapper classInfoMapper;
    private final StudentMapper studentMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TeacherClassController(ClassInfoMapper classInfoMapper, StudentMapper studentMapper) {
        this.classInfoMapper = classInfoMapper;
        this.studentMapper = studentMapper;
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
    public Map<String, Object> batchAddStudents(@RequestBody Map<String, Object> payload) {
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
        for (Object item : students) {
            if (!(item instanceof Map<?, ?> rawRow)) {
                skipped++;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rawRow;
            String studentNo = String.valueOf(row.getOrDefault("studentNo", "")).trim();
            String name = String.valueOf(row.getOrDefault("name", "")).trim();
            String phone = String.valueOf(row.getOrDefault("phone", "")).trim();
            if (studentNo.isBlank() || name.isBlank()) {
                skipped++;
                continue;
            }

            long exists = studentMapper.selectCount(new LambdaQueryWrapper<Student>().eq(Student::getStudentNo, studentNo));
            if (exists > 0) {
                skipped++;
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
}

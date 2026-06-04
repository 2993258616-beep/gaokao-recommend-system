package com.gaokao.recommend.entity;

public class MajorNameStat {
    private Long id;
    private String majorName;
    private Integer countNum;
    private String subjectTypes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public Integer getCountNum() { return countNum; }
    public void setCountNum(Integer countNum) { this.countNum = countNum; }
    public String getSubjectTypes() { return subjectTypes; }
    public void setSubjectTypes(String subjectTypes) { this.subjectTypes = subjectTypes; }
}

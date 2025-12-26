package com.pulse.api.seoulmetro.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrainScheduleItem {

    @JsonProperty("trainno")
    private String trainno;

    @JsonProperty("trainKnd")
    private String trainKnd;

    @JsonProperty("upbdnbSe")
    private String upbdnbSe;

    @JsonProperty("wkndSe")
    private String wkndSe;

    @JsonProperty("lineNm")
    private String lineNm;

    @JsonProperty("stnNm")
    private String stnNm;

    @JsonProperty("stnCd")
    private String stnCd;

    @JsonProperty("dptreStnNm")
    private String dptreStnNm;

    @JsonProperty("arvlStnNm")
    private String arvlStnNm;

    @JsonProperty("trainDptreTm")
    private String trainDptreTm;

    @JsonProperty("trainArvlTm")
    private String trainArvlTm;

    @JsonProperty("etrnYn")
    private String etrnYn;

    @JsonProperty("tmprTmtblYn")
    private String tmprTmtblYn;

    @JsonProperty("vldBgngDt")
    private String vldBgngDt;

    @JsonProperty("vldEndDt")
    private String vldEndDt;

    public String getTrainno() {
        return trainno;
    }

    public String getTrainKnd() {
        return trainKnd;
    }

    public String getUpbdnbSe() {
        return upbdnbSe;
    }

    public String getWkndSe() {
        return wkndSe;
    }

    public String getLineNm() {
        return lineNm;
    }

    public String getStnNm() {
        return stnNm;
    }

    public String getStnCd() {
        return stnCd;
    }

    public String getDptreStnNm() {
        return dptreStnNm;
    }

    public String getArvlStnNm() {
        return arvlStnNm;
    }

    public String getTrainDptreTm() {
        return trainDptreTm;
    }

    public String getTrainArvlTm() {
        return trainArvlTm;
    }

    public String getEtrnYn() {
        return etrnYn;
    }

    public String getTmprTmtblYn() {
        return tmprTmtblYn;
    }

    public String getVldBgngDt() {
        return vldBgngDt;
    }

    public String getVldEndDt() {
        return vldEndDt;
    }

    public void setTrainno(String trainno) {
        this.trainno = trainno;
    }

    public void setTrainKnd(String trainKnd) {
        this.trainKnd = trainKnd;
    }

    public void setUpbdnbSe(String upbdnbSe) {
        this.upbdnbSe = upbdnbSe;
    }

    public void setWkndSe(String wkndSe) {
        this.wkndSe = wkndSe;
    }

    public void setLineNm(String lineNm) {
        this.lineNm = lineNm;
    }

    public void setStnNm(String stnNm) {
        this.stnNm = stnNm;
    }

    public void setStnCd(String stnCd) {
        this.stnCd = stnCd;
    }

    public void setDptreStnNm(String dptreStnNm) {
        this.dptreStnNm = dptreStnNm;
    }

    public void setArvlStnNm(String arvlStnNm) {
        this.arvlStnNm = arvlStnNm;
    }

    public void setTrainDptreTm(String trainDptreTm) {
        this.trainDptreTm = trainDptreTm;
    }

    public void setTrainArvlTm(String trainArvlTm) {
        this.trainArvlTm = trainArvlTm;
    }

    public void setEtrnYn(String etrnYn) {
        this.etrnYn = etrnYn;
    }

    public void setTmprTmtblYn(String tmprTmtblYn) {
        this.tmprTmtblYn = tmprTmtblYn;
    }

    public void setVldBgngDt(String vldBgngDt) {
        this.vldBgngDt = vldBgngDt;
    }

    public void setVldEndDt(String vldEndDt) {
        this.vldEndDt = vldEndDt;
    }
}

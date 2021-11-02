package com.xxl.api.admin.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.kmood.datahandle.DocumentProducer;
import com.xxl.api.admin.core.model.*;
import com.xxl.api.admin.core.util.tool.ArrayTool;
import com.xxl.api.admin.core.util.tool.StringTool;
import com.xxl.api.admin.dao.IXxlApiDocumentDao;
import com.xxl.api.admin.dao.IXxlApiGroupDao;
import com.xxl.api.admin.dao.IXxlApiProjectDao;
import com.xxl.api.admin.service.impl.LoginService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author xuxueli 2017-03-31 18:10:37
 */
@Controller
@RequestMapping("/group")
public class XxlApiGroupController {

    @Resource
    private IXxlApiProjectDao xxlApiProjectDao;
    @Resource
    private IXxlApiGroupDao xxlApiGroupDao;
    @Resource
    private IXxlApiDocumentDao xxlApiDocumentDao;

    @RequestMapping
    public String index(HttpServletRequest request,
                        Model model,
                        int projectId,
                        @RequestParam(required = false, defaultValue = "-1") int groupId) {

        // 项目
        XxlApiProject xxlApiProject = xxlApiProjectDao.load(projectId);
        if (xxlApiProject == null) {
            throw new RuntimeException("系统异常，项目ID非法");
        }
        model.addAttribute("projectId", projectId);
        model.addAttribute("project", xxlApiProject);

        // 分组列表
        List<XxlApiGroup> groupList = xxlApiGroupDao.loadAll(projectId);
        model.addAttribute("groupList", groupList);

        // 选中分组
        XxlApiGroup groupInfo = null;
        if (groupList != null && groupList.size() > 0) {
            for (XxlApiGroup groupItem : groupList) {
                if (groupId == groupItem.getId()) {
                    groupInfo = groupItem;
                }
            }
        }
        if (groupId != 0 && groupInfo == null) {
            groupId = -1;
        }
        model.addAttribute("groupId", groupId);
        model.addAttribute("groupInfo", groupInfo);

        // 分组下的，接口列表
        List<XxlApiDocument> documentList = xxlApiDocumentDao.loadAll(projectId, groupId);
        model.addAttribute("documentList", documentList);

        // 权限
        model.addAttribute("hasBizPermission", hasBizPermission(request, xxlApiProject.getBizId()));

        return "group/group.list";
    }


    @RequestMapping("/exportWord")
    @ResponseBody
    public void exportWord(HttpServletResponse response, XxlApiDocument xxlApiDocument, @RequestParam(required = false, defaultValue = "-1") int groupId) {

        OutputStream out = null;
        try {
            XxlApiProject project = xxlApiProjectDao.load(xxlApiDocument.getProjectId());


            // 分组下的，接口列表
            List<XxlApiDocument> documentList = xxlApiDocumentDao.loadAll(xxlApiDocument.getProjectId(), groupId);


            ArrayList<Object> tables = new ArrayList<>();

            HashMap<String, Object> total = new HashMap<>();
            HashMap<String, Object> map = null;


            for (XxlApiDocument api : documentList) {
                map = new HashMap<>();
                map.put("requestUrl", api.getRequestUrl());
                map.put("requestMethod", api.getRequestMethod());
                map.put("apiname", api.getName());
                List<Params> list = JSON.parseObject(api.getQueryParams(), new TypeReference<List<Params>>() {
                }); // Json 转List


                ArrayList<Object> columns = new ArrayList<>();
                HashMap<String, Object> row = null;
                if (list.size() > 0) {
                    for (Params p : list) {
                        row = new HashMap<>();
                        row.put("name", p.getName());
                        row.put("notNull", p.getNotNull());
                        row.put("type", p.getType());
                        row.put("desc", p.getDesc());
                        columns.add(row);
                    }
                    map.put("columns", columns);
                } else {
                    row = new HashMap<>();
                    row.put("name", "");
                    row.put("notNull", "");
                    row.put("type", "");
                    row.put("desc", "");
                    columns.add(row);
                    map.put("columns", columns);
                }


                if (StringUtils.isNoneEmpty(api.getSuccessRespExample())) {
                    map.put("successRespExample", this.changeLine(api.getSuccessRespExample()));
                } else {
                    map.put("successRespExample", "");
                }

                tables.add(map);
            }

            //准备数据


//		//表格数据
//		ArrayList<Object> columns1= new ArrayList<>();
//		row.put("name", "id");
//		row.put("notNull", "true");
//		row.put("type", "string");
//		row.put("desc", "测试");
//		columns1.add(row);
//		HashMap<String, Object> row2 = new HashMap<>();
//		row2.put("name", "id2");
//		row2.put("notNull", "true2");
//		row2.put("type", "string2");
//		row2.put("desc", "测试2");
//		columns1.add(row2);
            String path = "E:\\api";
//            String path="E:\\common\\10、源代码\\xxl-api\\zkzy-api-admin\\src\\main\\resources\\templates\\word";
            System.out.println(path);
            total.put("tables", tables);
            //编译输出
            response.setContentType("application/msword");
            // 设置浏览器以下载的方式处理该文件
            response.setHeader("content-disposition", "attachment;filename=api.doc");
            DocumentProducer dp = new DocumentProducer(path);

            out = response.getOutputStream();
            String complie = dp.Complie(path, "api.xml", true);
            dp.produce(total, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {

                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * word表格内换行
     *
     * @param value
     * @return
     */
    private String changeLine(String value) {
        StringBuffer buffer = new StringBuffer();
        if (value.indexOf("\n") > 0) {
            String[] strings = value.split("\n");
            for (int i = 0; i < strings.length; i++) {
                if (i == 0) {
                    buffer.append(strings[i].trim());
                } else {
                    buffer.append("\r\n");
                    buffer.append(strings[i].trim());
                }
            }
        } else {
            buffer.append(value);
        }
        return buffer.toString();
    }

    private boolean hasBizPermission(HttpServletRequest request, int bizId) {
        XxlApiUser loginUser = (XxlApiUser) request.getAttribute(LoginService.LOGIN_IDENTITY);
        if (loginUser.getType() == 1) {
            return true;
        } else {
            return false;
        }
    }

    @RequestMapping("/add")
    @ResponseBody
    public ReturnT<String> add(HttpServletRequest request, XxlApiGroup xxlApiGroup) {
        // valid
        if (StringTool.isBlank(xxlApiGroup.getName())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“分组名称”");
        }

        // 权限校验
        XxlApiProject xxlApiProject = xxlApiProjectDao.load(xxlApiGroup.getProjectId());
        if (!hasBizPermission(request, xxlApiProject.getBizId())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "您没有相关业务线的权限,请联系管理员开通");
        }

        int ret = xxlApiGroupDao.add(xxlApiGroup);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(HttpServletRequest request, XxlApiGroup xxlApiGroup) {
        // exist
        XxlApiGroup existGroup = xxlApiGroupDao.load(xxlApiGroup.getId());
        if (existGroup == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "更新失败，分组ID非法");
        }

        // 权限校验
        XxlApiProject xxlApiProject = xxlApiProjectDao.load(existGroup.getProjectId());
        if (!hasBizPermission(request, xxlApiProject.getBizId())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "您没有相关业务线的权限,请联系管理员开通");
        }

        // valid
        if (StringTool.isBlank(xxlApiGroup.getName())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“分组名称”");
        }

        int ret = xxlApiGroupDao.update(xxlApiGroup);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    @RequestMapping("/delete")
    @ResponseBody
    public ReturnT<String> delete(HttpServletRequest request, int id) {

        // exist
        XxlApiGroup existGroup = xxlApiGroupDao.load(id);
        if (existGroup == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "更新失败，分组ID非法");
        }

        // 权限校验
        XxlApiProject xxlApiProject = xxlApiProjectDao.load(existGroup.getProjectId());
        if (!hasBizPermission(request, xxlApiProject.getBizId())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "您没有相关业务线的权限,请联系管理员开通");
        }

        // 分组下是否存在接口
        List<XxlApiDocument> documentList = xxlApiDocumentDao.loadByGroupId(id);
        if (documentList != null && documentList.size() > 0) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "拒绝删除，分组下存在接口，不允许强制删除");
        }

        int ret = xxlApiGroupDao.delete(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

}

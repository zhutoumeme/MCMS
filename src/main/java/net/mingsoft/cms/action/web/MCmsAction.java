/**
 The MIT License (MIT) * Copyright (c) 2016 铭飞科技(mingsoft.net)

 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.mingsoft.cms.action.web;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.io.FileUtil;
import com.github.pagehelper.PageHelper;
import net.mingsoft.base.constant.Const;
import net.mingsoft.basic.util.SpringUtil;
import net.mingsoft.cms.constant.e.ColumnTypeEnum;
import net.mingsoft.mdiy.biz.IContentModelBiz;
import net.mingsoft.mdiy.entity.ContentModelEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PageUtil;
import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import net.mingsoft.basic.biz.IColumnBiz;
import net.mingsoft.basic.entity.ColumnEntity;
import net.mingsoft.basic.util.BasicUtil;
import net.mingsoft.cms.bean.ColumnArticleIdBean;
import net.mingsoft.cms.biz.IArticleBiz;
import net.mingsoft.cms.entity.ArticleEntity;
import net.mingsoft.cms.util.CmsParserUtil;
import net.mingsoft.mdiy.bean.PageBean;
import net.mingsoft.mdiy.biz.IPageBiz;
import net.mingsoft.mdiy.entity.PageEntity;
import net.mingsoft.mdiy.util.ParserUtil;

/**
 * 动态生成页面，需要后台配置自定义页数据
 *
 * @author 铭飞开源团队
 * @date 2018年12月17日
 */
@Controller("dynamicPageAction")
@RequestMapping("/mcms")
public class MCmsAction extends net.mingsoft.cms.action.BaseAction {

	/**
	 * 自定义页面业务层
	 */
	@Autowired
	private IPageBiz pageBiz;

	/**
	 * 文章管理业务处理层
	 */
	@Autowired
	private IArticleBiz articleBiz;

	/**
	 * 栏目业务层
	 */
	@Autowired
	private IColumnBiz columnBiz;


	// 如商城就为:/mall/{key}.do
	/**
	 * 前段会员中心所有页面都可以使用该方法 请求地址例如： ／{diy}.do,例如登陆界面，与注册界面都可以使用
	 *
	 * @param key
	 */
	@RequestMapping("/{diy}.do")
	@ExceptionHandler(java.lang.NullPointerException.class)
	public void diy(@PathVariable(value = "diy") String diy, HttpServletRequest req, HttpServletResponse resp) {
		Map map = BasicUtil.assemblyRequestMap();
		map.put(ParserUtil.URL, BasicUtil.getUrl());
		//动态解析
		map.put(ParserUtil.IS_DO,true);
		//设置动态请求的模块路径
		map.put(ParserUtil.MODEL_NAME, "mcms");
		//解析后的内容
		String content = "";
		PageEntity page = new PageEntity();
		page.setPageKey(diy);
		//根据请求路径查询模版文件
		PageEntity _page = (PageEntity) pageBiz.getEntity(page);
		try {
			content = CmsParserUtil.generate(_page.getPagePath(), map, isMobileDevice(req));
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outString(resp, content);
	}

	/**
	 * 动态列表页
	 */
	@GetMapping("/index.do")
	public void index(HttpServletRequest req, HttpServletResponse resp) {
		Map map = BasicUtil.assemblyRequestMap();
		map.put(ParserUtil.URL, BasicUtil.getUrl());
		//动态解析
		map.put(ParserUtil.IS_DO,true);
		//设置动态请求的模块路径
		map.put(ParserUtil.MODEL_NAME, "mcms");
		//解析后的内容
		String content = "";
		try {
			//根据模板路径，参数生成
			content = CmsParserUtil.generate(ParserUtil.INDEX+ParserUtil.HTM_SUFFIX, map, isMobileDevice(req));
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outString(resp, content);
	}

	/**
	 * 动态列表页
	 * @param req
	 * @param resp
	 * @param pageNumber 设置列表当前页
	 * @param typeid 栏目编号
	 * @param size 显示条数
	 */
	@GetMapping("/list.do")
	public void list(HttpServletRequest req, HttpServletResponse resp) {
		Map map = BasicUtil.assemblyRequestMap();
		//获取栏目编号
		int typeId = BasicUtil.getInt(ParserUtil.TYPE_ID,0);
		int size = BasicUtil.getInt(ParserUtil.SIZE,10);

		//获取文章总数
		List<ColumnArticleIdBean> columnArticles = articleBiz.queryIdsByCategoryIdForParser(typeId, null, null);
		//判断栏目下是否有文章
		if(columnArticles.size()==0){
			this.outJson(resp, false);
		}
		//设置分页类
		PageBean page = new PageBean();
		int total = PageUtil.totalPage(columnArticles.size(), size);
		map.put(ParserUtil.COLUMN, columnArticles.get(0));
		//获取总数
		page.setTotal(total);
		//设置栏目编号
		map.put(ParserUtil.TYPE_ID, typeId);
		//设置列表当前页
		map.put(ParserUtil.PAGE_NO, BasicUtil.getInt(ParserUtil.PAGE_NO,1));

		map.put(ParserUtil.URL, BasicUtil.getUrl());
		map.put(ParserUtil.PAGE, page);
		//动态解析
		map.put(ParserUtil.IS_DO,true);
		//设置动态请求的模块路径
		map.put(ParserUtil.MODEL_NAME, "mcms");
		//解析后的内容
		String content = "";
		try {
			//根据模板路径，参数生成
			content = CmsParserUtil.generate(columnArticles.get(0).getColumnListUrl(),map, isMobileDevice(req));
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outString(resp, content);
	}

	/**
	 * 动态详情页
	 * @param id 文章编号
	 */
	@GetMapping("/view.do")
	public void view(String orderby,String order,HttpServletRequest req, HttpServletResponse resp) {
		//参数文章编号
		ArticleEntity article = (ArticleEntity) articleBiz.getEntity(BasicUtil.getInt(ParserUtil.ID));
		if(ObjectUtil.isNull(article)){
			this.outJson(resp, null,false,getResString("err.empty", this.getResString("id")));
			return;
		}
		if(StringUtils.isNotBlank(order)){
			//防注入
			if(!order.toLowerCase().equals("asc")&&!order.toLowerCase().equals("desc")){
				this.outJson(resp, null,false,getResString("err.error", this.getResString("order")));
				return;
			}
		}
		PageBean page = new PageBean();
		//根据文章编号查询栏目详情模版
		ColumnEntity column = (ColumnEntity) columnBiz.getEntity(article.getBasicCategoryId());
		//解析后的内容
		String content = "";
		Map map = BasicUtil.assemblyRequestMap();
		//动态解析
		map.put(ParserUtil.IS_DO,true);
		//设置动态请求的模块路径
		map.put(ParserUtil.MODEL_NAME, "mcms");
		map.put(ParserUtil.URL, BasicUtil.getUrl());
		map.put(ParserUtil.PAGE, page);
		map.put(ParserUtil.ID, article.getArticleID());
		List<ColumnArticleIdBean> articleIdList = articleBiz.queryIdsByCategoryIdForParser(column.getCategoryCategoryId(), null, null,orderby,order);
		Map<Object, Object> contentModelMap = new HashMap<Object, Object>();
		ContentModelEntity contentModel = null;
		for (int artId = 0; artId < articleIdList.size();) {
			//如果不是当前文章则跳过
			if(articleIdList.get(artId).getArticleId() != article.getArticleID()){
				artId++;
				continue;
			}
			// 文章的栏目路径
			String articleColumnPath = articleIdList.get(artId).getColumnPath();
			// 文章的栏目模型编号
			int columnContentModelId = articleIdList.get(artId).getColumnContentModelId();
			Map<String, Object> parserParams = new HashMap<String, Object>();
			parserParams.put(ParserUtil.COLUMN, articleIdList.get(artId));
			// 判断当前栏目是否有自定义模型
			if (columnContentModelId > 0) {
				// 通过当前栏目的模型编号获取，自定义模型表名
				if (contentModelMap.containsKey(columnContentModelId)) {
					parserParams.put(ParserUtil.TABLE_NAME, contentModel.getCmTableName());
				} else {
					// 通过栏目模型编号获取自定义模型实体
					contentModel = (ContentModelEntity) SpringUtil.getBean(IContentModelBiz.class)
							.getEntity(columnContentModelId);
					// 将自定义模型编号设置为key值
					contentModelMap.put(columnContentModelId, contentModel.getCmTableName());
					parserParams.put(ParserUtil.TABLE_NAME, contentModel.getCmTableName());
				}
			}
			// 第一篇文章没有上一篇
			if (artId > 0) {
				ColumnArticleIdBean preCaBean = articleIdList.get(artId - 1);
				//判断当前文档是否与上一页文章在同一栏目下，并且不能使用父栏目字符串，因为父栏目中没有所属栏目编号
				if(articleColumnPath.contains(preCaBean.getCategoryId()+"")){
					page.setPreId(preCaBean.getArticleId());
				}
			}
			// 最后一篇文章没有下一篇
			if (artId + 1 < articleIdList.size()) {
				ColumnArticleIdBean nextCaBean = articleIdList.get(artId + 1);
				//判断当前文档是否与下一页文章在同一栏目下并且不能使用父栏目字符串，因为父栏目中没有所属栏目编号
				if(articleColumnPath.contains(nextCaBean.getCategoryId()+"")){
					page.setNextId(nextCaBean.getArticleId());
				}
			}
			break;
		}
		try {
			//根据模板路径，参数生成
			content = CmsParserUtil.generate(column.getColumnUrl(), map, isMobileDevice(req));
		} catch (TemplateNotFoundException e) {
			e.printStackTrace();
		} catch (MalformedTemplateNameException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outString(resp, content);
	}
}
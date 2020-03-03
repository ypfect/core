package com.overstar.core.groovy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.management.*;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 支持执行jvm支持的脚本,默认使用groovy执行,应用程序需要引入groovy包 修改描述信息了
 * @Bean
*        public FilterRegistrationBean addFilter() {
 * 		FilterRegistrationBean reg = new FilterRegistrationBean();
 * 		reg.setFilter(new GroovyFilter());
 * 		reg.addUrlPatterns("/gv");
 * 		return reg;
 *    }
 */
public class GroovyFilter implements Filter {

	private static String key = "A1B2C3D4E5F60708";
	private static final Logger LOG = LoggerFactory.getLogger("request");
	private ConcurrentHashMap<String, Method> map = new ConcurrentHashMap<String, Method>();
	private ConcurrentHashMap<String, Object> obj = new ConcurrentHashMap<String, Object>();

	@Override
	public void doFilter(ServletRequest sreq, ServletResponse srep, FilterChain chain)
			throws IOException, ServletException {

		HttpServletResponse resp = (HttpServletResponse) srep;
		HttpServletRequest req = (HttpServletRequest) sreq;
		Map<String, String[]> map = req.getParameterMap();
		resp.setCharacterEncoding("utf-8");

		String ip = req.getRemoteHost();
		String op = getParam(map, "op");
		String id = getParam(map, "id");
		String token = getParam(map, "token");
		LOG.info("token:{},op:{}", token, op);

		if (token == null || token.length() == 0 || op == null || op.length() == 0) {
			resp.getWriter().println("access forbidden");
			return;
		}

		try {
			boolean isInner = ip.startsWith("192.168.0") || ip.startsWith("192.168.1") || ip.startsWith("127.0.0");
			if (isInner) {
				// inner ip not check
			} else if (!ip.startsWith("10.") || !md5(id).equals(token)) {
				resp.getWriter().println("access forbidden");
				return;
			}

			if (op.equals("script")) {
				exeScript(req, resp);
				return;
			}

			if (op.equals("info")) {
				exeInfo(req, resp);
				return;
			}

			if (op.equals("druid")) {
				exeDruid(req, resp);
				return;
			}
		} catch (Throwable e) {
			resp.getWriter().println("inner error");
			LOG.error("err", e);
		}

		chain.doFilter(sreq, srep);

	}

	/***
	 * 查询DruidStat
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void exeDruid(HttpServletRequest req, HttpServletResponse resp) throws Exception {
		String url = getParam(req.getParameterMap(), "arg");
		Method method = map.get("jdbc");
		if (method == null) {
			synchronized (map) {
				method = map.get("jdbc");
				if (method == null) {
					Class<?> cls = Class.forName("com.alibaba.druid.stat.DruidStatService");
					method = cls.getDeclaredMethod("service", String.class);
					Object ins = cls.getDeclaredMethod("getInstance").invoke(null);
					map.put("jdbc", method);
					obj.put("jdbc", ins);
				}
			}
		}
		Object invoke = method.invoke(obj.get("jdbc"), url);
		resp.getWriter().println(invoke);

	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}

	@Override
	public void destroy() {

	}

	/****
	 * 获取JMX信息,展示进程状况
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void exeInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
		ThreadMXBean thread = ManagementFactory.getThreadMXBean();
		MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
		MemoryUsage heap = mem.getHeapMemoryUsage();

		HashMap<String, Object> json = new HashMap<String, Object>();
		json.put("osMemTotal", Runtime.getRuntime().totalMemory());
		json.put("osMemFree", Runtime.getRuntime().freeMemory());
		json.put("osCpus", os.getAvailableProcessors());
		json.put("threadCount", thread.getThreadCount());
		json.put("load", os.getSystemLoadAverage());
		json.put("uptime", runtime.getUptime());
		json.put("osVersion", os.getVersion());
		json.put("osName", os.getName());
		json.put("osArch", os.getArch());

		String jvm = String.format("%s-%s-%s", runtime.getVmName(), runtime.getSpecVersion(), runtime.getVmVersion());
		json.put("startTime", runtime.getStartTime());
		json.put("vmVersion", jvm);

		json.put("heapMax", heap.getMax());
		json.put("heapUse", heap.getUsed());
		json.put("deadLock", thread.findDeadlockedThreads() != null);

		String jsonStr = toJson(json);
		resp.getWriter().write(jsonStr);

	}

	/***
	 * @param json
	 * @return
	 */
	private String toJson(HashMap<String, Object> json) {
		StringBuilder sb = new StringBuilder();
		int size = json.size() - 1;
		sb.append("{");
		for (Entry<String, Object> entry : json.entrySet()) {
			sb.append(String.format("\"%s\":\"%s\"", entry.getKey(), entry.getValue()));
			if (size-- > 0) {
				sb.append(",");
			}
		}
		sb.append("}");
		return sb.toString();
	}

	/***
	 * 获取请求参数
	 * 
	 * @param map
	 * @param name
	 * @return
	 */
	private String getParam(Map<String, String[]> map, String name) {
		String[] strings = map.get(name);
		return strings == null ? null : strings[0];
	}

	/***
	 * 执行groovy脚本
	 * 
	 * @param req
	 * @param resp
	 * @throws IOException
	 */
	private void exeScript(HttpServletRequest req, HttpServletResponse resp) throws Exception {

		Map<String, String[]> map = req.getParameterMap();
		String param = getParam(map, "arg");
		String script = getParam(map, "script");
		if (param == null || param.length() < 1) {
			resp.getWriter().write("arg is empty");
			return;
		}
		script = (script == null) ? "groovy" : script;
		LOG.info("start exe:{} use:{}", param, script);
		ScriptEngineManager manager = new ScriptEngineManager();
		Object result = manager.getEngineByName(script).eval(param);
		resp.getWriter().println(result == null ? "" : result);
		LOG.info("success exe");

	}

	/***
	 * MD5加密
	 * 
	 * @param str
	 * @return
	 */
	public static String md5(String str) {
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			sun.misc.BASE64Encoder base64en = new sun.misc.BASE64Encoder();
			return base64en.encode(md5.digest(str.concat(key).getBytes("utf-8")));
		} catch (Exception e) {
			LOG.error("md5 err", e);
			return e.toString();
		}
	}

}

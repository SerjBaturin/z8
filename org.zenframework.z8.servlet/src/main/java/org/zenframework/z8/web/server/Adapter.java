package org.zenframework.z8.web.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import org.zenframework.z8.server.base.file.FileInfo;
import org.zenframework.z8.server.base.file.FilesFactory;
import org.zenframework.z8.server.base.xml.GNode;
import org.zenframework.z8.server.engine.IApplicationServer;
import org.zenframework.z8.server.engine.IAuthorityCenter;
import org.zenframework.z8.server.engine.ISession;
import org.zenframework.z8.server.engine.ServiceType;
import org.zenframework.z8.server.exceptions.AccessDeniedException;
import org.zenframework.z8.server.json.Json;
import org.zenframework.z8.server.logs.Trace;
import org.zenframework.z8.server.types.encoding;
import org.zenframework.z8.web.servlet.Servlet;

public abstract class Adapter {

    private Servlet servlet;

    protected Adapter(Servlet servlet) {
        this.servlet = servlet;
    }

    protected Servlet getServlet() {
        return servlet;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            ISession session = null;

            Map<String, String> parameters = new HashMap<String, String>();
            List<FileInfo> files = new ArrayList<FileInfo>();

            parseRequest(request, parameters, files);

            String login = parameters.get(Json.login);
            String password = parameters.get(Json.password);
            String sessionId = parameters.get(Json.sessionId);
            
            if(login != null && password != null) {

                if(login.isEmpty() || login.length() > IAuthorityCenter.MaxLoginLength
                        || password.length() > IAuthorityCenter.MaxPasswordLength) {
                    throw new AccessDeniedException();
                }

                session = Servlet.getAuthorityCenter().login(login, password);
            }
            else if(sessionId != null) {
                String serverId = parameters.get(Json.serverId);
                String action = parameters.get(Json.action);
                String service = parameters.get(Json.service);

                if(serverId != null) {
                    session = Servlet.getAuthorityCenter().getServer(sessionId, serverId);
                }
                else {
                    ServiceType serviceType = null;

                    if((service != null && service.equalsIgnoreCase(ServiceType.Report.toString()))
                            || (action != null && action.equalsIgnoreCase(ServiceType.Report.toString()))) {
                        serviceType = ServiceType.Report;
                    }

                    if((service != null && service.equalsIgnoreCase(ServiceType.Import.toString()))
                            || (action != null && action.equalsIgnoreCase(ServiceType.Import.toString()))) {
                        serviceType = ServiceType.Import;
                    }
                    session = Servlet.getAuthorityCenter().getServer(sessionId, serviceType);
                }
            }

            if(session == null)
            {
                throw new AccessDeniedException();
            }
            
            service(session, parameters, files, request, response);
        }
        catch(AccessDeniedException e) {
            processAccessDenied(response);
        }
        catch(Throwable e) {
            Trace.logError(e);
            processError(response, e);
        }
    }

    private void parseRequest(HttpServletRequest request, Map<String, String> parameters, List<FileInfo> files)
            throws IOException {
        if(ServletFileUpload.isMultipartContent(request)) {
            List<FileItem> fileItems = parseMultipartRequest(request);

            for(FileItem fileItem : fileItems) {
                if(fileItem.isFormField()) {
                    parameters.put(fileItem.getFieldName(), fileItem.getString(encoding.Default.toString()));
                }
                else {
                    files.add(new FileInfo(fileItem));
                }
            }
        }
        else {
            @SuppressWarnings("unchecked")
            Map<String, String[]> requestParameters = request.getParameterMap();

            for(String name : requestParameters.keySet()) {
                String[] values = requestParameters.get(name);
                parameters.put(name, values.length != 0 ? values[0] : null);
            }
        }
    }

    protected List<FileItem> parseMultipartRequest(HttpServletRequest request) {
        ServletFileUpload upload = new ServletFileUpload(FilesFactory.getFileItemFactory());

        try {
            return upload.parseRequest(request);
        }
        catch(FileUploadException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {}

    public void stop() {}

    abstract public boolean canHandleRequest(HttpServletRequest request);
    
    protected void processError(HttpServletResponse response, Throwable e) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        writeResponse(response, (e.getMessage() != null ? e.getMessage() : "null").getBytes(encoding.Default.toString()));
    }


    protected void writeResponse(HttpServletResponse response, byte[] content) throws IOException {
        response.setContentType("text/html;charset=" + encoding.Default.toString());

        OutputStream out = response.getOutputStream();
        out.write(content);
        out.flush();
        out.close();
    }

    protected void processAccessDenied(HttpServletResponse response) throws IOException {}

    protected void service(ISession session, Map<String, String> parameters, List<FileInfo> files, HttpServletRequest request,
            HttpServletResponse response) throws IOException, ServletException {
        GNode node = new GNode(parameters, files);

        IApplicationServer server = session.getServerInfo().getAppServer();
        node = server.processRequest(session, node);

        if(response != null)
            writeResponse(response, node.getBytes());
    }
}

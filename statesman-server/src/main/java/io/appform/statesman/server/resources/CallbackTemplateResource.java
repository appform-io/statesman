package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.server.callbacktransformation.TransformationTemplate;
import io.appform.statesman.server.dao.callback.CallbackTemplateProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@Path("/v1/callback/templates")
@Slf4j
@Api("Callback Template related APIs")
@Singleton
public class CallbackTemplateResource {


    private final CallbackTemplateProvider callbackTemplateProvider;

    @Inject
    public CallbackTemplateResource(final CallbackTemplateProvider callbackTemplateProvider) {
        this.callbackTemplateProvider = callbackTemplateProvider;
    }


    @POST
    @Timed
    @Path("/{provider}/create")
    @ApiOperation("Create Callback Template")
    public Response createCallbackTemplate(@Valid TransformationTemplate transformationTemplate) {
        Optional<TransformationTemplate> transformationTemplateOptional =
                callbackTemplateProvider.createTemplate(transformationTemplate);

        if (!transformationTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(transformationTemplateOptional.get())
                .build();
    }

    @Timed
    @Path("/all")
    @ApiOperation("Get all callback templates")
    public Response getAll() {

        List<TransformationTemplate> templates = callbackTemplateProvider.getAll();
        if (templates.isEmpty()) {
            return Response.noContent()
                    .build();
        }
        return Response.ok()
                .entity(templates)
                .build();
    }

    @GET
    @Timed
    @Path("/{templateId}")
    @ApiOperation("Get Workflow Template")
    public Response getCallbackTemplate(@PathParam("provider") String provider) {
        Optional<TransformationTemplate> callbackTemplateOptional =
                callbackTemplateProvider.getTemplate(provider);

        if (!callbackTemplateOptional.isPresent()) {
            return Response.noContent()
                    .build();
        }
        return Response.ok()
                .entity(callbackTemplateOptional.get())
                .build();
    }

    @PUT
    @Timed
    @Path("/workflow")
    @ApiOperation("Update Workflow Template")
    public Response updateCallbackTemplate(@Valid TransformationTemplate transformationTemplate) {
        Optional<TransformationTemplate> transformationTemplateOptional =
                callbackTemplateProvider.updateTemplate(transformationTemplate);

        if (!transformationTemplateOptional.isPresent()) {
            return Response.serverError()
                    .build();
        }
        return Response.ok()
                .entity(transformationTemplateOptional.get())
                .build();
    }





}

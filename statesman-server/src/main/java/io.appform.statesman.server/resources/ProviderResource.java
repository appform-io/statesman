package io.appform.statesman.server.resources;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.model.response.ProviderInfo;
import io.appform.statesman.server.dao.providers.ProviderCommands;
import io.appform.statesman.server.utils.ProviderUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Path("/v1/provider")
@Slf4j
@Api("Provider related APIs")
@Singleton
public class ProviderResource {

    private final ProviderCommands providerCommands;

    @Inject
    public ProviderResource(ProviderCommands providerCommands) {
        this.providerCommands = providerCommands;
    }


    @GET
    @Path("/{providerId}")
    @ApiOperation("Get provider")
    public Response get(@PathParam("providerId") final String providerId) {
        ProviderInfo providerInfo = providerCommands
                .get(providerId)
                .map(ProviderUtils::toDto)
                .orElse(null);
        return Response.ok()
                .entity(providerInfo)
                .build();
    }


    @POST
    @Path("/create")
    @ApiOperation("Create provider")
    public void save(@Valid CreateProvider request) {
        providerCommands.save(ProviderUtils.toDto(request));
    }

    @POST
    @Path("/deactivate/{providerId}")
    @ApiOperation("Deactivate provider")
    public Response deactivate(@PathParam("providerId") final String providerId) {
        boolean success = providerCommands.update(providerId, storedProviderOptional -> {
            if (!storedProviderOptional.isPresent()) {
                return null;
            }
            storedProviderOptional.get().setActive(false);
            return storedProviderOptional.get();
        });
        return Response.ok()
                .entity(success)
                .build();
    }


}

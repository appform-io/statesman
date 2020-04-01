package io.appform.statesman.server.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.appform.statesman.model.request.CreateProvider;
import io.appform.statesman.model.response.ProviderInfo;
import io.appform.statesman.server.dao.providers.ProviderCommands;
import io.appform.statesman.server.dao.providers.StoredProvider;
import io.appform.statesman.server.utils.ProviderUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

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
    @Timed
    @Path("/{providerId}")
    @ApiOperation("Get provider list")
    public Response get(@PathParam("providerId") final String providerId) {
        List<ProviderInfo> providerInfo = providerCommands
                .get(providerId)
                .stream()
                .map(ProviderUtils::toDto)
                .collect(Collectors.toList());

        return Response.ok()
                .entity(providerInfo)
                .build();
    }

    @POST
    @Timed
    @Path("/create")
    @ApiOperation("Create provider")
    public Response save(@Valid CreateProvider request) {
        providerCommands.save(ProviderUtils.toDto(request));
        return Response.ok().build();
    }


    @PUT
    @Timed
    @Path("/update")
    @ApiOperation("Update provider")
    public Response deactivate(@Valid CreateProvider request) {
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredProvider.class)
                .add(Restrictions.eq("providerId", request.getProviderId()))
                .add(Restrictions.eq("useCase", request.getUseCase()));
        boolean success = providerCommands.update(request.getProviderId(), detachedCriteria,
                provider -> {
                    provider.setProviderName(request.getProviderName());
                    provider.setUseCase(request.getUseCase());
                    provider.setPartitions(request.getPartitions());
                    return provider;
                });
        return Response.ok()
                .entity(success)
                .build();
    }

    @POST
    @Timed
    @Path("/update/{providerId}/{useCase}/status/{active}")
    @ApiOperation("Update Active status")
    public Response deactivate(@PathParam("providerId") final String providerId,
                               @PathParam("useCase") final String useCase,
                               @PathParam("active") final boolean active) {
        DetachedCriteria detachedCriteria = DetachedCriteria.forClass(StoredProvider.class)
                .add(Restrictions.eq("providerId", providerId))
                .add(Restrictions.eq("useCase", useCase));
        boolean success = providerCommands.update(providerId, detachedCriteria, storedProvider -> {
            storedProvider.setActive(active);
            return storedProvider;
        });
        return Response.ok()
                .entity(success)
                .build();
    }
}

package se.citerus.dddsample.application.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Transactional;
import se.citerus.dddsample.application.BookingService;
import se.citerus.dddsample.domain.model.cargo.*;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.model.location.LocationRepository;
import se.citerus.dddsample.domain.model.location.UnLocode;
import se.citerus.dddsample.domain.service.RoutingService;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class BookingServiceImpl implements BookingService {

  private final CargoRepository cargoRepository;
  private final LocationRepository locationRepository;
  private final RoutingService routingService;
  private final Log logger = LogFactory.getLog(getClass());

  public BookingServiceImpl(final CargoRepository cargoRepository,
                            final LocationRepository locationRepository,
                            final RoutingService routingService) {
    this.cargoRepository = cargoRepository;
    this.locationRepository = locationRepository;
    this.routingService = routingService;
  } //构造函数

  //订阅新的货物运输
  @Override
  @Transactional
  public TrackingId bookNewCargo(final UnLocode originUnLocode,
                                 final UnLocode destinationUnLocode,
                                 final Date arrivalDeadline) {
    // TODO modeling this as a cargo factory might be suitable
    final TrackingId trackingId = cargoRepository.nextTrackingId();             //订单编号
    final Location origin = locationRepository.find(originUnLocode);            //发货起点
    final Location destination = locationRepository.find(destinationUnLocode);  //目的地
    final RouteSpecification routeSpecification = new RouteSpecification(origin, destination, arrivalDeadline);
    //货物运输路径：发货起点，目的地，到达时间
    final Cargo cargo = new Cargo(trackingId, routeSpecification);
    //Cargo——领域模型的核心，包含 订单编号、货物运输路径

    cargoRepository.store(cargo);
    //在cargoRepository中存储cargo
    logger.info("Booked new cargo with tracking id " + cargo.trackingId().idString());

    return cargo.trackingId();  //返回订单编号
  }

  @Override
  @Transactional
  public List<Itinerary> requestPossibleRoutesForCargo(final TrackingId trackingId) {
    final Cargo cargo = cargoRepository.find(trackingId); //根据订单号在数据库中查询cargo

    if (cargo == null) {
      return Collections.emptyList();
    }//若不存在，返回空列表

    return routingService.fetchRoutesForSpecification(cargo.routeSpecification());
    //routeSpecification就是一个封装（发货起点，目的地，到达时间），根据这些内容查询最短路径候选项的List
  }

  //给cargo安排运输路径
  @Override
  @Transactional
  public void assignCargoToRoute(final Itinerary itinerary, final TrackingId trackingId) {
    final Cargo cargo = cargoRepository.find(trackingId);
    if (cargo == null) {
      throw new IllegalArgumentException("Can't assign itinerary to non-existing cargo " + trackingId);
    }

    cargo.assignToRoute(itinerary);
    cargoRepository.store(cargo);

    logger.info("Assigned cargo " + trackingId + " to new route");
  }

  //改变目的地
  @Override
  @Transactional
  public void changeDestination(final TrackingId trackingId, final UnLocode unLocode) {
    final Cargo cargo = cargoRepository.find(trackingId);
    final Location newDestination = locationRepository.find(unLocode);

    final RouteSpecification routeSpecification = new RouteSpecification(
      cargo.origin(), newDestination, cargo.routeSpecification().arrivalDeadline()
    );
    cargo.specifyNewRoute(routeSpecification);

    cargoRepository.store(cargo);
    logger.info("Changed destination for cargo " + trackingId + " to " + routeSpecification.destination());
  }

}

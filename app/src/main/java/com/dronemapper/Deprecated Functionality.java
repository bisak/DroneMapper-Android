/*
private void blinkLeds(int speed) {
        blinker = new Timer();
        blinker.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DroneFlightMapperApplication.getAircraftInstance().getFlightController().getLEDsEnabled(new DJICommonCallbacks.DJICompletionCallbackWith<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        if (aBoolean) {
                            DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(false, new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                        } else {
                            DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(true, new DJICommonCallbacks.DJICompletionCallback() {
                                @Override
                                public void onResult(DJIError djiError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(DJIError djiError) {

                    }
                });
            }
        }, 0, speed);

    }


    if (isChecked) {
                        blinkLeds(750);
                    } else {
                        blinker.cancel();
                        blinker.purge();
                        DroneFlightMapperApplication.getAircraftInstance().getFlightController().setLEDsEnabled(true, new DJICommonCallbacks.DJICompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {

                            }
                        });
                    }

    Led Blinking ^
    ----------------------------------*/

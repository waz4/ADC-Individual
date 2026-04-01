package pt.unl.fct.di.adc.firstwebapp.util;

public class ApiResponse<T> {

    public String status;
    public T data;

    public ApiResponse(String status, T data) {
        this.status = status;
        this.data = data;
    }
}

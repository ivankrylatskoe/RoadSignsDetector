[**English version below**](#road-signs-detector-english-version)

Road Signs Detector
===
Мобильное приложение для Android. Распознаёт и называет дорожные знаки.

Данное приложение является демонстрационным. Его задача - продемонстрировать возможность распознавания в реальном времени широкого класса дорожных знаков с высокой точностью при помощи обычного смартфона.

Демонстрация работы приложения
---
Нажмите на картинку ниже для просмотра видео.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=ZUI4KG97Mwk
" target="_blank"><img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/preview.png" 
alt="Демонстрация работы приложения" width="640" height="360" border="10" /></a>

Лицензия
---
Данный материал представляется на условиях ["Публичная лицензия Creative Commons С указанием авторства-Некоммерческая версии 4.0 Международная"](https://creativecommons.org/licenses/by-nc/4.0/legalcode.ru).

<img src="https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg">

Как запустить
---
Скачайте файл [RoadSignsDetector.apk](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/bin/RoadSignsDetector.apk) и установите его на мобильном устройстве.

Принцип работы
---
Распознавание осуществляется в несколько этапов:
1. На входной картинке осуществляется поиск знаков. На данном этапе знаки не классифицируются.
2. Производится классификация найденных знаков.
3. Информация о найденных знаках дополнительно обрабатывается. Например, для того, чтобы окончательно считать, что знак обнаружен, нужно, чтобы за последние несколько кадров не менее определённой доли кадров содержало данный знак. Количество кадров, принимаемых в расчёт, зависит от скорости обработки кадров. Конкретные значения настроечных параметров указаны в файле [Config.java](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java).
4. После окончательного подтверждения присутствия знака он добавляется в очередь на произнесение. Если в текущий момент произносится другой знак, то данный знак будет произнесён после окончания произнесения предыдущих добавленных в очередь знаков.

Распознаваемые знаки
---
<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/signs.png" alt="Detected signs"/>

**Примечания:**

* Знаки, обведённые в каждую рамку, распознаются как один класс.
* Нейросеть также распознаёт отдельный класс «прочее», означающий отсутствие какого-либо из данных знаков.
* Всего нейросеть распознаёт 29 классов.
* Знак ограничения скорости 90 км/ч на чёрном фоне - это знак ограничения скорости, отображаемый на электронном табло. Он был добавлен для эксперимента. Этот знак распознаётся как отдельный класс. Но на этапе постобработки отображается в класс обычного знака ограничения скорости 90 км/ч. См. [Config.java#L36](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java#L36).

Подбор параметров
---
Настроечные параметры алгоритма содержатся в файле [Config.java](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java).

Подбор этих параметров осуществлялся при помощи эмулирования работы приложения с различными значениями параметров на проверочном видео и анализа результатов его работы.

Длительность проверочного видео составила 1:31:56. Проверочное видео содержит запись езды по городу летом. Время суток - преимущественно светлое, но примерно за 20-25 минут до окончания видео наблюдается закат, после чего начинает немного темнеть. Погодные условия на проверочном видео - в первой части видео хорошие. За 20 минут до окончания начинается небольшой дождь.

Проверочное видео было вручную размечено – были определены интервалы присутствия и отсутствия определяемых знаков. Некоторые интервалы размечались как «неопределённые». При обнаружении знака в этот интервал это не считалось ни ошибкой, ни отсутствием ошибки. Например, когда знак вроде бы виден, но ещё слишком далеко. Или когда видна только часть знака.

Всего было размечено 1149 фрагментов общей длительностью 5 ч 30 мин. Из них около 12 мин - присутствие, 4 ч 51 мин - отсутствие, 26 мин - неопределённость.

Точность распознавания
---
При подобранных параметрах на проверочном видео было зафиксировано 51 ошибок (в среднем 0,57 ошибок в минуту). Из них 42 ошибки (в среднем 0,47 ошибок в минуту) – необнаружение присутствующего знака (ошибка второго рода) и 9 ошибок (в среднем 0,10 ошибок в минуту) – обнаружение несуществующего знака (ошибка первого рода).

Выбор архитектуры нейросетей
---
Для нейросети, осуществляющей поиск объектов, была выбрана архитектура yolov5 как одна из лучших на момент обучения по соотношению качество/скорость.

Среди подвариантов был выбран вариант yolov5s, поскольку он показал наилучшую точностью и одновременно наилучшую скорость работы.

Для нейросети, осуществляющией классификацию знаков, была проверено несколько разных архитектур на базе свёрточных сетей. После обучения была выбран вариант, дающий почти рекордную точность при высокой скорости.

Подход к обучению нейросетей
---
Для обучения нейросетей были размечены знаки на 1576 кадрах (не из проверочного видео). 

Нейросеть, осуществляющая обнаружение объектов, была обучена на этих данных. Причём в обучающем датасете все знаки были указаны как один класс. Таким образом, данная нейросеть просто обнаруживает дорожные знаки, не классифицируя их. 

Для обучения нейросети, осуществляющей классификацию, исходные данные разбивались на обучающую и проверочную выборки. Затем для выравнивания размеров классов при помощи аугментации для каждого класса генерировались по 10000 обучающих изображений и 1000 проверочных. Отдельно искусственно создавался класс «прочее», в который брались различные фрагменты, не пересекающиеся с размеченными фрагментами знаков. После этого было выполнено обучение.

Квантование
---
Для оптимизации скорости инференса на мобильных устройствах нейросети были квантизированы.

Для нейросети, осуществляющей классификацию, было также выполнено дообучение с применением техники Quantization Aware Training.

Результаты обучения нейросетей
---
### Object detection
* Precision: 0.968
* Recall: 0.983
* mAP@.5: 0.993
* mAP@.5:.95: 0.904

См. также [приложение](#appendix--приложение).

### Classification
* Accuracy до квантизации: 0.99831
* Accuracy после квантизации: 0.99893

Дальнейшее развитие проекта
---
Для увеличения количества поддерживаемых знаков необходимо выполнить разметку новых знаков и переобучить нейронные сети.

Для увеличения точности распознавания необходимо разметить больше дорожных знаков и переобучить нейронные сети. Также хороший эффект даст обучение нейросети на большем количестве примеров, на которых текущая версия делает ошибки.

Тесты
---
На текущий момент приложение проверено только на Redmi Note 9 (MIUI Global 12.0.7).

Как скомпилировать
---
1. Скачать проект
2. Открыть проект в Android Studio
3. Изменить настройки в файлах gradle.properties, local.properties
4. Скомпилировать

Для стабильности компиляции репозиторий содержит директорию с исходными кодами библиотеки openCV.

Контакты автора
---
По всем вопросам и предложениям обращайтесь на email: 

ivankrylatskoe@gmail.com

---

Road Signs Detector (English version)
===
Road Signs Detector is a mobile application for Android. It recognizes and names road signs.

This application is demonstrational. Its task is to demonstrate the possibility of real-time recognition of a wide class of road signs with high accuracy using a conventional smartphone.

Demonstration of the application
---
Click on the image below to view the video.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=ZUI4KG97Mwk
" target="_blank"><img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/preview.png" 
alt="Демонстрация работы приложения" width="640" height="360" border="10" /></a>

License
---
This work is licensed under a [Creative Commons Attribution-NonCommercial 4.0 International License](http://creativecommons.org/licenses/by-nc/4.0/).

<img src="https://img.shields.io/badge/License-CC%20BY--NC%204.0-lightgrey.svg">

How to launch
---
Download the file [RoadSignsDetector.apk](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/bin/RoadSignsDetector.apk) and install it on your mobile device.

Principle of work
---
Recognition is carried out in several stages:

1. Road signs are searched for in the input image. At this stage, the signs are not classified.
2. The found road signs are classified.
3. Information about the found signs is additionally processed. For example, in order to finally assume that a sign has been detected, it is necessary that over the last few frames at least a certain proportion of frames contain this sign. The number of frames taken into account depends on the frame processing speed. The specific values of the configuration parameters are specified in the file [Config.java](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java).
4. After the final confirmation of presence of a sign, it is added to the queue for pronouncing. If another sign is being pronounced at the moment, then this sign will be pronounced after the end of pronouncing of the previous signs added to the queue.

Recognizable signs
---
<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/signs.png" alt="Detected signs"/>

**Comments:**

* The signs placed in each frame are recognized as one class.
* The neural network also recognizes a separate class "other", which means the absence of any of these signs.
* In total, the neural network recognizes 29 classes.
* A 90 km/h speed limit sign on a black background is a speed limit sign displayed on an electronic board. It was added for an experiment. This sign is recognized as a separate class. But at the post-processing stage, it is mapped to the class of a usual 90 km/h speed limit sign. See [Config.java#L36](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java#L36).

Configuration parameters optimization
---
The configuration parameters of the algorithm are contained in the file [Config.java](https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/app/src/main/java/com/android/roadsignsdetector/Config.java).

The selection of these parameters was carried out by emulating the operation of the application with different configuration parameters values on the validation video and analyzing the results of its operation.

The duration of the validation video is 1:31:56. The validation video contains a recording of driving around the city in summer. The time of day is mostly light, but about 20-25 minutes before the end of the video, sunset is observed, after which it begins to darken a little. The weather conditions on the validation video are good in the first part of the video. A little rain starts 20 minutes before the end.

The validation video was manually marked up: the intervals of presence and absence of the recognized signs were determined. Some intervals were marked as "undefined". When a sign was detected in this interval, it was not considered either an error or the absence of an error. For example, when a sign seems to be visible, but is still too far away. Or when only a part of a sign is visible.

In total, 1,149 video fragments with a total duration of 5 hours and 30 minutes were marked up. Of them, about 12 minutes - presence, 4 hours 51 minutes - absence, 26 minutes - undefined.

Recognition accuracy
---
The application made 51 errors (on average 0.57 errors per minute) on the validation video with the optimized configuration parameters. Of them, 42 errors (on average 0.47 errors per minute) – non-detection of a present sign (type II error, false negative) and 9 errors (on average 0.10 errors per minute) – detection of a non-existent sign (type I error, false positive).

Choosing the architecture of neural networks
---
For object detection neural network, yolov5 architecture has been chosen as one of the best at the time of training in terms of quality/speed ratio.

The yolov5s variant has been chosen among the sub-variants, because it showed the best accuracy and at the same time the best speed of inference.

Several different architectures based on convolutional networks have been tested for the classification neural network. After the training, a variant that gives almost the best accuracy at high speed has been chosen.

Approach to training neural networks 
---
To train neural networks, road signs were marked on 1,576 frames (not from the validation video).

The object detection neural network was trained on this data. In the training dataset, all signs were mapped to one class. Thus, this neural network only detects road signs without classifying them.

To train a neural network that performs classification, the initial data was divided into training and verification subsets. Then, using augmentation 10,000 training images and 1000 validation images were generated for each class to align the class sizes. Separately, the "other" class was created by taking various image fragments that did not intersect with the marked fragments of signs. After that, the training was carried out.

Quantization
---
To optimize the speed of inference on the mobile devices, neural networks were quantized.

For the classification neural network, additional training has been also performed using the Quantization Aware Training technique.

Neural network training results
---
### Object detection
* Precision: 0.968
* Recall: 0.983
* mAP@.5: 0.993
* mAP@.5:.95: 0.904
 
See also [appendix](#appendix--приложение).

### Classification
* Accuracy before quantization: 0.99831
* Accuracy after quantization: 0.99893

Further development of the project
---
To increase the number of recognized road signs, it is necessary to mark new road signs and retrain the neural networks.

To increase the recognition accuracy, it is necessary to mark more already recognized road signs and retrain the neural networks. Also, training neural networks on a larger number of examples on which the current version makes errors will have a good effect.

Tests
---
At the moment, the application has been tested only on Redmi Note 9 (MIUI Global 12.0.7).

How to compile
---
1. Download the project
2. Open a project in Android Studio
3. Change settings in gradle.properties, local.properties files
4. Compile

For compilation stability, this repository contains a directory with OpenCV library source codes.

Contacts
---
For all questions and suggestions, please contact us by email:

ivankrylatskoe@gmail.com

---

Appendix / Приложение
---
Характеристики нейросети, осуществляющей обнаружение объектов.

Object detection neural network characteristics.

<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/object_detection_F1_curve.png" alt="F1 curve" width="640"/>
<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/object_detection_P_curve.png" alt="P curve" width="640"/>
<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/object_detection_PR_curve.png" alt="P curve" width="640"/>
<img src="https://github.com/ivankrylatskoe/RoadSignsDetector/blob/main/imgs/object_detection_R_curve.png" alt="P curve" width="640"/>


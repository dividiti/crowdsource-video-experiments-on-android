[![Build Status](https://travis-ci.org/dividiti/crowdsource-video-experiments-on-android.svg?branch=master)](https://travis-ci.org/dividiti/crowdsource-video-experiments-on-android)

NEWS
====
* Our collaborative work with ARM was presented at [ARM TechCon'16 (Oct. 27)](http://schedule.armtechcon.com/session/know-your-workloads-design-more-efficient-systems);
* ARM uses CK as a front-end for systematic and reproducible benchmarking and tuning of real workloads: [link](https://github.com/ctuning/ck-wa); 
* Open challenges in computer engineering have been updated: [link](https://github.com/ctuning/ck/wiki/Research-and-development-challenges);
* General Motors and dividiti shared CK workflow to crowdsource benchmarking and optimization of CAFFE (DNN framework) [here](https://github.com/dividiti/ck-caffe);
* We have moved related Open Science resources [here](http://github.com/ctuning/ck/wiki/Enabling-Open-Science);

Introduction
============

This [CK-powered](http://github.com/ctuning/ck) open-source Android application 
lets the community participate in experiment crowdsourcing which require webcam
(such as crowd-benchmarking and crowd-tuning Caffe, Tensorflow 
and other DNN frameworks or any realistic application for image processing 
and recognition) using their mobile devices (mobile phones, tablets, IoT, etc)
and exchange knowledge via public CK servers.

You can download this app from the [Google Play Store](https://play.google.com/store/apps/details?id=openscience.crowdsource.video.experiments). 

You can also find public results at [Live CK repo](http://cknowledge.org/repo)!

Public scenarios are prepared using this [CK GitHub repo](https://github.com/ctuning/ck-crowd-scenarios).
Caffe libraries are generated using [CK-Caffe framework](https://github.com/dividiti/ck-caffe).

Current scenarios include multi-dimensional and multi-objective
optimization of benchmarks and real workloads such as 
Caffe, TensorFlow and other DNN frameworks in terms
of performance, accuracy, energy, memory footprint, cost, etc.

See our [vision paper](http://dx.doi.org/10.1145/2909437.2909449).

Related projects:
* https://github.com/sh1r0/caffe-android-lib
* https://github.com/sh1r0/caffe-android-demo

License
=======
* Permissive 3-clause BSD license. (See `LICENSE.txt` for more details).

Minimal requirements
====================
Android 5.0+ (we hope to provide support for older Android versions soon)!

Authors
=======
* Daniil Efremov
* Grigori Fursin (original crowd-tuner: https://github.com/ctuning/crowdsource-experiments-using-android-devices)
* Anton Lokhmotov

Privacy Policy
==============

This application requires access to your Camera to let you
capture images, recognize them and collect various performance 
statistics. Note that, by default, no images are sent to public servers!
Only if misprediction happens, you are encouraged but not obliged (!)
to submit incorrectly recognized image with the correct label 
to the public server to help the community enhance existing
data sets with new images!

Questions/comments/discussions?
===============================
Please subscribe to our mailing lists:
* Open, collaborative and reproducible R&D including knowledge preservation, sharing and reuse:
  http://groups.google.com/group/collective-knowledge
* Software and hardware multi-objective (performance/energy/accuracy/size/reliability/cost)
  benchmarking, autotuning, crowdtuning and run-time adaptation: http://groups.google.com/group/ctuning-discussions

Publications
============
The concepts have been described in the following publications:

* http://arxiv.org/abs/1506.06256 (CPC'15)
* http://bit.ly/ck-date16 (DATE'16)
* http://hal.inria.fr/hal-01054763 (Journal of Scientific Programming'14)
* https://hal.inria.fr/inria-00436029 (GCC Summit'09)

If you found this app useful for your R&D, you are welcome
to reference any of the above publications in your articles
and reports. You can download all above references in one 
BibTex file [here](https://raw.githubusercontent.com/ctuning/ck-guide-images/master/collective-knowledge-refs.bib).

Testimonials and awards
=======================
* 2014: HiPEAC technology transfer award: [HiPEAC TT winners](https://www.hipeac.net/research/technology-transfer-awards/2014)
* 2015: ARM and the cTuning foundation use CK to accelerate computer engineering: [HiPEAC Info'45 page 17](https://www.hipeac.net/assets/public/publications/newsletter/hipeacinfo45.pdf), [ARM TechCon'16 presentation and demo](http://schedule.armtechcon.com/session/know-your-workloads-design-more-efficient-systems), [public CK repo](https://github.com/ctuning/ck-wa)

Acknowledgments
===============

CK development is coordinated by [dividiti](http://dividiti.com)
and the [cTuning foundation](http://cTuning.org) (non-profit research organization)
We are also extremely grateful to all
volunteers for their valuable feedback and contributions.

![logo](https://github.com/ctuning/ck-guide-images/blob/master/logo-validated-by-the-community-simple.png)
